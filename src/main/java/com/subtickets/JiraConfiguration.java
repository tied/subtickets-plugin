package com.subtickets;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.bc.ServiceResult;
import com.atlassian.jira.bc.config.StatusService;
import com.atlassian.jira.bc.project.ProjectCreationData;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.IssueTypeService;
import com.atlassian.jira.config.StatusCategoryManager;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.roles.ProjectRoleImpl;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.subtickets.Constants.StatusCategoryName;
import com.subtickets.servlet.DataSourceConfig;
import com.subtickets.servlet.JiraDBConfig;
import org.ofbiz.core.entity.GenericEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.subtickets.Constants.FieldNames.ACTUAL_COSTS;
import static com.subtickets.Constants.FieldNames.FUND_COLLECTION_MANNER;
import static com.subtickets.Constants.FieldNames.FUND_TYPE;
import static com.subtickets.Constants.FieldNames.PLANNED_COSTS;
import static com.subtickets.Constants.FieldNames.ROOM;
import static com.subtickets.Constants.FieldNames.ROOMER;
import static com.subtickets.Constants.FieldNames.VOTE_SQUARE;
import static com.subtickets.Constants.IssueTypesNames.IMPROVEMENT;
import static com.subtickets.Constants.IssueTypesNames.INCIDENT;
import static com.subtickets.Constants.IssueTypesNames.NOTIFICATION;
import static com.subtickets.Constants.IssueTypesNames.PAYMENT;
import static com.subtickets.Constants.IssueTypesNames.PAYMENT_NOTIFY;
import static com.subtickets.Constants.IssueTypesNames.SUB_TASK;
import static com.subtickets.Constants.IssueTypesNames.TASK;
import static com.subtickets.Constants.IssueTypesNames.VOTING;
import static com.subtickets.Constants.IssueTypesNames.VOTING_NOTIFY;
import static com.subtickets.Constants.IssueTypesNames.VOTING_SESSION;
import static com.subtickets.Constants.ProjectValues.KEY;
import static com.subtickets.Constants.ProjectValues.NAME;
import static com.subtickets.Constants.StatusCategoryName.DONE;
import static com.subtickets.Constants.StatusCategoryName.IN_PROGRESS;
import static com.subtickets.Constants.StatusCategoryName.TO_DO;

@Component
public class JiraConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JiraConfiguration.class);

    public static ApplicationUser admin;

    public static Project project;

    public static Map<String, IssueType> issueTypes = new HashMap<>();

    public static Map<String, CustomField> customFields = new HashMap<>();

    public static Map<StatusCategoryName, StatusCategory> statusCategories = new HashMap<>();

    public static Map<StatusCategoryName, String> statusCategoriesIcons = new HashMap<>();

    public JiraConfiguration() {
        admin = ComponentAccessor.getUserManager().getUserByName("admin");
        bootstrap();
    }

    private void bootstrap() {
        initDBTable();
        createProject();
        createProjectRoles();
        createIssueTypes();
        createCustomFields();
        createStatuses();
    }

    private void initDBTable() {
        DataSourceConfig dataSourceConfig = parseDbConfig().dataSourceConfig;
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName(dataSourceConfig.driveClassName);
            conn = DriverManager.getConnection(dataSourceConfig.url, dataSourceConfig.username, dataSourceConfig.password);
            stmt = conn.createStatement();
            boolean tableExists = conn.getMetaData().getTables(null, null, "cwd_user_pass", null).next();
            if (!tableExists) {
                stmt.executeUpdate("CREATE table cwd_user_pass " +
                        "(pwd_hash character varying(255) NOT NULL, " +
                        "fk_user_id numeric(18,0) NOT NULL, " +
                        "change_stamp timestamp without time zone DEFAULT now(), " +
                        "CONSTRAINT \"PK_cwd_user_pass\" PRIMARY KEY (fk_user_id, pwd_hash), " +
                        "CONSTRAINT \"FK_cwd_user_id\" FOREIGN KEY (fk_user_id) " +
                        "REFERENCES cwd_user (id) MATCH SIMPLE ON UPDATE CASCADE ON DELETE CASCADE) " +
                        "WITH (OIDS=FALSE);");
            } else {
                log.warn("Table cwd_user_pass exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JiraDBConfig parseDbConfig() {
        try {
            String catalinaBase = System.getenv("CATALINA_BASE");
            File dbConfig = new File(new File(catalinaBase).getParent(), "Application Data/JIRA/dbconfig.xml");
            JAXBContext context = JAXBContext.newInstance(JiraDBConfig.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (JiraDBConfig) unmarshaller.unmarshal(dbConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createProject() {
        ProjectService projectService = ComponentAccessor.getComponent(ProjectService.class);
        ProjectService.CreateProjectValidationResult createProjectValidationResult = projectService.validateCreateProject(admin, new ProjectCreationData.Builder()
                .withName(NAME)
                .withKey(KEY)
                .withType("business")
                .withLead(admin)
                .withAssigneeType(AssigneeTypes.UNASSIGNED)
                .build());
        if (createProjectValidationResult.isValid()) {
            project = projectService.createProject(createProjectValidationResult);
        } else {
            logErrors(createProjectValidationResult.getErrorCollection());
            project = projectService.getProjectByKey(admin, KEY).get();
        }
    }

    private void createProjectRoles() {
        createProjectRole("Accountant");
        createProjectRole("CEO");
        createProjectRole("CTO");
        createProjectRole("Contractor");
        createProjectRole("PQI");
        createProjectRole("Roomer");
    }

    private void createProjectRole(String name) {
        ProjectRoleService projectRoleService = ComponentAccessor.getComponent(ProjectRoleService.class);
        SimpleErrorCollection errors = new SimpleErrorCollection();
        projectRoleService.createProjectRole(admin, new ProjectRoleImpl(name, null), errors);
        logErrors(errors);
    }

    private void createIssueTypes() {
        createIssueType(IMPROVEMENT);
        createIssueType(INCIDENT);
        createIssueType(NOTIFICATION);
        createIssueType(PAYMENT);
        createIssueType(TASK);
        createIssueType(VOTING);
        createIssueType(VOTING_SESSION);
        createSubIssueType(PAYMENT_NOTIFY);
        createSubIssueType(SUB_TASK);
        createSubIssueType(VOTING_NOTIFY);
        issueTypes.values().forEach(issueType -> {
            Avatar avatar = issueType.getAvatar();
            log.error(issueType.getName() + ": " + (avatar != null ? avatar.getFileName() : ""));
        });
    }

    private void createIssueType(String name) {
        createIssueType(name, false);
    }

    private void createSubIssueType(String name) {
        createIssueType(name, true);
    }

    private void createIssueType(String name, boolean subTask) {
        IssueTypeService issueTypeService = ComponentAccessor.getComponent(IssueTypeService.class);
        IssueTypeService.IssueTypeCreateInput issueTypeCreateInput = new IssueTypeService.IssueTypeCreateInput.Builder()
                .setName(name)
                .setType(subTask ? IssueTypeService.IssueTypeCreateInput.Type.SUBTASK : IssueTypeService.IssueTypeCreateInput.Type.STANDARD)
                .build();
        IssueTypeService.CreateValidationResult createValidationResult = issueTypeService.validateCreateIssueType(admin, issueTypeCreateInput);
        IssueType issueType;
        if (createValidationResult.isValid()) {
            issueType = issueTypeService.createIssueType(admin, createValidationResult).getIssueType();
        } else {
            logErrors(createValidationResult.getErrorCollection());
            issueType = findIssueType(name);
        }
        if (issueType == null) {
            throw new RuntimeException("Couldn't find issue type " + name);
        }
        issueTypes.put(name, issueType);
    }

    private IssueType findIssueType(String name) {
        return ComponentAccessor.getComponent(IssueTypeManager.class)
                .getIssueTypes()
                .stream()
                .filter(issueType -> issueType.getName().equalsIgnoreCase(name))
                .findFirst()
                .get();
    }

    private void createCustomFields() {
        createNumberField(ACTUAL_COSTS);
        createSelectField(FUND_COLLECTION_MANNER);
        createSelectField(FUND_TYPE);
        createNumberField(PLANNED_COSTS);
        createTextField(ROOM);
        createTextField(ROOMER);
        createNumberField(VOTE_SQUARE);
    }

    private void createTextField(String name) {
        createCustomField(name, "textfield", "textsearcher");
    }

    private void createNumberField(String name) {
        createCustomField(name, "float", "exactnumber");
    }

    private void createSelectField(String name) {
        createCustomField(name, "select", "multiselectsearcher");
    }

    private void createCustomField(String name, String type, String searcher) {
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        Collection<CustomField> customFieldObjectsByName = customFieldManager.getCustomFieldObjectsByName(name);
        CustomField result = null;
        if (customFieldObjectsByName.isEmpty()) {
            try {
                result = customFieldManager.createCustomField(name, null,
                        customFieldManager.getCustomFieldType("com.atlassian.jira.plugin.system.customfieldtypes:" + type),
                        customFieldManager.getCustomFieldSearcher("com.atlassian.jira.plugin.system.customfieldtypes:" + searcher),
                        null, null);
            } catch (GenericEntityException e) {
                e.printStackTrace();
            }
        } else {
            log.warn("Custom field with the name \"" + name + "\" already exists");
            result = customFieldObjectsByName.iterator().next();
        }
        customFields.put(name, result);
    }

    private void createStatuses() {
        initStatusCategories();
        createStatus("TO DO", TO_DO);
        createStatus("OPEN", TO_DO);
        createStatus("VOTING", IN_PROGRESS);
        createStatus("IN PROGRESS", IN_PROGRESS);
        createStatus("REVIEW", IN_PROGRESS);
        createStatus("RESOLVED", DONE);
        createStatus("DONE", DONE);
        createStatus("CLOSED", DONE);
        createStatus("CONFIRM", DONE);
    }

    private void initStatusCategories() {
        StatusCategoryManager statusCategoryManager = ComponentAccessor.getComponent(StatusCategoryManager.class);
        StatusManager statusManager = ComponentAccessor.getComponent(StatusManager.class);
        List<StatusCategoryName> categories = Arrays.asList(StatusCategoryName.values());
        categories.forEach(statusCategory -> statusCategories.put(statusCategory, statusCategoryManager.getStatusCategoryByName(statusCategory.value)));
        statusCategories.forEach((key, value) -> statusCategoriesIcons.put(key, statusManager.getStatuses().stream().filter(status -> status.getStatusCategory().equals(value))
                .findFirst().get().getSimpleStatus().getIconUrl()));
    }

    private void createStatus(String name, StatusCategoryName category) {
        StatusService statusService = ComponentAccessor.getComponent(StatusService.class);
        String iconUrl = statusCategoriesIcons.get(category);
        StatusCategory statusCategory = statusCategories.get(category);
        ServiceResult validateCreateStatus = statusService.validateCreateStatus(admin, name, "", iconUrl, statusCategory);
        if (validateCreateStatus.isValid()) {
            statusService.createStatus(admin, name, "", iconUrl, statusCategory);
        } else {
            logErrors(validateCreateStatus.getErrorCollection());
        }
    }

    private void logErrors(ErrorCollection errors) {
        if (errors.hasAnyErrors()) {
            if (errors.getErrors().size() > 0) {
                log.warn(Arrays.toString(errors.getErrors().entrySet().toArray()));
            }
            if (errors.getErrorMessages().size() > 0) {
                log.warn(Arrays.toString(errors.getErrorMessages().toArray()));
            }
        }
    }

}
