package com.subtickets;

import com.atlassian.jira.bc.ServiceResult;
import com.atlassian.jira.bc.config.StatusService;
import com.atlassian.jira.bc.project.ProjectCreationData;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.IssueTypeService;
import com.atlassian.jira.config.StatusCategoryManager;
import com.atlassian.jira.config.StatusManager;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldException;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenImpl;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme;
import com.atlassian.jira.issue.fields.screen.FieldScreenSchemeImpl;
import com.atlassian.jira.issue.fields.screen.FieldScreenSchemeItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenSchemeItemImpl;
import com.atlassian.jira.issue.fields.screen.FieldScreenSchemeManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeEntityImpl;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.operation.ScreenableIssueOperation;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.workflow.AssignableWorkflowScheme;
import com.atlassian.jira.workflow.ConfigurableJiraWorkflow;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.WorkflowSchemeManager;
import com.google.common.base.CaseFormat;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import com.opensymphony.workflow.loader.WorkflowLoader;
import com.subtickets.Constants.FieldAvailability;
import com.subtickets.Constants.Groups;
import com.subtickets.Constants.StatusCategoryName;
import com.subtickets.servlet.DataSourceConfig;
import com.subtickets.servlet.JiraDBConfig;
import org.ofbiz.core.entity.GenericEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.jira.component.ComponentAccessor.getComponent;
import static com.atlassian.jira.component.ComponentAccessor.getConstantsManager;
import static com.atlassian.jira.component.ComponentAccessor.getCustomFieldManager;
import static com.atlassian.jira.component.ComponentAccessor.getFieldManager;
import static com.atlassian.jira.component.ComponentAccessor.getFieldScreenManager;
import static com.atlassian.jira.component.ComponentAccessor.getGroupManager;
import static com.atlassian.jira.component.ComponentAccessor.getIssueTypeScreenSchemeManager;
import static com.atlassian.jira.component.ComponentAccessor.getOptionsManager;
import static com.atlassian.jira.component.ComponentAccessor.getProjectManager;
import static com.atlassian.jira.component.ComponentAccessor.getUserManager;
import static com.atlassian.jira.component.ComponentAccessor.getWorkflowManager;
import static com.atlassian.jira.component.ComponentAccessor.getWorkflowSchemeManager;
import static com.atlassian.jira.issue.customfields.CustomFieldUtils.buildJiraIssueContexts;
import static com.subtickets.Constants.FieldAvailability.CREATE;
import static com.subtickets.Constants.FieldAvailability.EDIT;
import static com.subtickets.Constants.FieldAvailability.VIEW;
import static com.subtickets.Constants.FieldNames.ACCOUNT_NUMBER;
import static com.subtickets.Constants.FieldNames.ACTUAL_COSTS;
import static com.subtickets.Constants.FieldNames.ACT_END_DATE;
import static com.subtickets.Constants.FieldNames.ACT_START_DATE;
import static com.subtickets.Constants.FieldNames.ASSIGNEE;
import static com.subtickets.Constants.FieldNames.BUSINESS_ADDRESS;
import static com.subtickets.Constants.FieldNames.BUSINESS_MAIL;
import static com.subtickets.Constants.FieldNames.CONTRACTOR;
import static com.subtickets.Constants.FieldNames.CONTRACTOR_BANK;
import static com.subtickets.Constants.FieldNames.CONTRACTOR_BANK_ID;
import static com.subtickets.Constants.FieldNames.CONTRACTOR_ID;
import static com.subtickets.Constants.FieldNames.CONTRACTOR_NAME;
import static com.subtickets.Constants.FieldNames.DESCRIPTION;
import static com.subtickets.Constants.FieldNames.DUE_DATE;
import static com.subtickets.Constants.FieldNames.EDRPOU;
import static com.subtickets.Constants.FieldNames.FUND_COLLECTION_MANNER;
import static com.subtickets.Constants.FieldNames.FUND_TYPE;
import static com.subtickets.Constants.FieldNames.LABELS;
import static com.subtickets.Constants.FieldNames.PHONE;
import static com.subtickets.Constants.FieldNames.PLANNED_COSTS;
import static com.subtickets.Constants.FieldNames.PRIORITY;
import static com.subtickets.Constants.FieldNames.ROOM;
import static com.subtickets.Constants.FieldNames.ROOMER;
import static com.subtickets.Constants.FieldNames.SUMMARY;
import static com.subtickets.Constants.FieldNames.VOTE_SQUARE;
import static com.subtickets.Constants.IssueTypesNames.BOARDING;
import static com.subtickets.Constants.IssueTypesNames.BOARDING_VALIDATION;
import static com.subtickets.Constants.IssueTypesNames.IMPROVEMENT;
import static com.subtickets.Constants.IssueTypesNames.INCIDENT;
import static com.subtickets.Constants.IssueTypesNames.NOTIFICATION;
import static com.subtickets.Constants.IssueTypesNames.PAYMENT;
import static com.subtickets.Constants.IssueTypesNames.PAYMENT_NOTIFY;
import static com.subtickets.Constants.IssueTypesNames.PUBLIC;
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
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Component
public class JiraConfiguration implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(JiraConfiguration.class);

    public static ApplicationUser admin;

    public static Project project;

    public static Map<String, IssueType> issueTypes = new HashMap<>();

    public static Map<String, CustomField> customFields = new HashMap<>();

    public static Map<StatusCategoryName, StatusCategory> statusCategories = new HashMap<>();

    public static Map<StatusCategoryName, String> statusCategoriesIcons = new HashMap<>();

    private static Map<String, Long> screensIds = new HashMap<>();

    private TemplateEngine templateEngine;

    @Override
    public void afterPropertiesSet() {
        templateEngine = new TemplateEngine();
        admin = getUserManager().getUserByName("admin");
        bootstrap();
    }

    private void bootstrap() {
        initDBTable();
        createProject();
        createUsersGroups();
        createIssueTypes();
        createCustomFields();
        createStatuses();
        createScreens();
        createWorkflows();
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
            String catalinaBase = System.getProperty("catalina.home");
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
        log.trace("Trying to create project");
        ProjectService projectService = getComponent(ProjectService.class);
        ProjectService.CreateProjectValidationResult createProjectValidationResult = projectService.validateCreateProject(admin, new ProjectCreationData.Builder()
                .withName(NAME)
                .withKey(KEY)
                .withType("business")
                .withLead(admin)
                .withAssigneeType(AssigneeTypes.UNASSIGNED)
                .build());
        if (createProjectValidationResult.isValid()) {
            project = projectService.createProject(createProjectValidationResult);
            log.debug("Successfully created project {}", NAME);
        } else {
            logErrors(createProjectValidationResult.getErrorCollection());
            project = projectService.getProjectByKey(admin, KEY).get();
        }
        log.trace("Finished creation of project");
    }

    private void createUsersGroups() {
        log.trace("Trying to create users groups");
        createUsersGroup(Groups.ACCOUNTANT);
        createUsersGroup(Groups.ADMIN);
        createUsersGroup(Groups.CEO);
        createUsersGroup(Groups.CTO);
        createUsersGroup(Groups.CONTRACTOR);
        createUsersGroup(Groups.PQI);
        createUsersGroup(Groups.ROOMER);
        log.trace("Finished creation of users groups");
    }

    private void createUsersGroup(String name) {
        log.trace("Trying to create users group {}", name);
        GroupManager groupManager = getGroupManager();
        if (!groupManager.groupExists(name)) {
            try {
                groupManager.createGroup(name);
                log.debug("Successfully created users group {}", name);
            } catch (Exception e) {
                log.error("Failed to create group with name {}. Message: {}", name, e.getMessage());
            }
        }
    }

    private void createIssueTypes() {
        log.trace("Trying to create issue types");
        createIssueType(IMPROVEMENT);
        createIssueType(INCIDENT);
        createIssueType(NOTIFICATION);
        createIssueType(PAYMENT);
        createIssueType(TASK);
        createIssueType(VOTING);
        createIssueType(VOTING_SESSION);
        createIssueType(PUBLIC);
        createIssueType(BOARDING);
        createSubIssueType(PAYMENT_NOTIFY);
        createSubIssueType(SUB_TASK);
        createSubIssueType(VOTING_NOTIFY);
        createSubIssueType(BOARDING_VALIDATION);
        log.trace("Finished creation of issue types");
    }

    private void createIssueType(String name) {
        createIssueType(name, false);
    }

    private void createSubIssueType(String name) {
        createIssueType(name, true);
    }

    private void createIssueType(String name, boolean subTask) {
        log.trace("Trying to create issue type {}", name);
        IssueTypeService issueTypeService = getComponent(IssueTypeService.class);
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
        log.trace("Finished creation of issue type {}", name);
    }

    private IssueType findIssueType(String name) {
        return getComponent(IssueTypeManager.class)
                .getIssueTypes()
                .stream()
                .filter(issueType -> issueType.getName().equalsIgnoreCase(name))
                .findFirst()
                .get();
    }

    private void createCustomFields() {
        log.trace("Trying to create custom fields");
        createNumberField(ACTUAL_COSTS);
        createSelectField(FUND_COLLECTION_MANNER, "Auto", "Door", "Sq");
        createSelectField(FUND_TYPE, "Esteblished", "Custom", "Development");
        createNumberField(PLANNED_COSTS);
        createTextField(ROOM);
        createUserField(ROOMER);
        createUserField(CONTRACTOR);
        createNumberField(VOTE_SQUARE);
        createDateField(ACT_START_DATE);
        createDateField(ACT_END_DATE);
        createTextField(CONTRACTOR_NAME);
        createUserField(CONTRACTOR_ID);
        createTextField(CONTRACTOR_BANK);
        createNumberField(CONTRACTOR_BANK_ID);
        createTextField(ACCOUNT_NUMBER);
        createTextField(BUSINESS_ADDRESS);
        createTextField(BUSINESS_MAIL);
        createTextField(PHONE);
        createNumberField(EDRPOU);
        log.trace("Finished creation of custom fields");
    }

    private void createTextField(String name) {
        createCustomField(name, "textfield", "textsearcher");
    }

    private void createNumberField(String name) {
        createCustomField(name, "float", "exactnumber");
    }

    private void createSelectField(String name, String... options) {
        CustomField field = createCustomField(name, "select", "multiselectsearcher");
        OptionsManager optionsManager = getOptionsManager();
        List<FieldConfigScheme> configurationSchemes = field.getConfigurationSchemes();
        FieldConfig config = configurationSchemes.listIterator().next().getOneAndOnlyConfig();
        Options existingOptions = optionsManager.getOptions(config);
        for (int i = 0, sequence = 0; i < options.length; i++, sequence++) {
            String option = options[i];
            if (existingOptions.stream().noneMatch(o -> o.getValue().equals(option))) {
                optionsManager.createOption(config, null, (long) sequence, option);
            } else {
                sequence++;
            }
        }
        List<String> newOptions = asList(options);
        existingOptions.stream().filter(option -> newOptions.stream().noneMatch(n -> n.equals(option.getValue()))).forEach(optionsManager::deleteOptionAndChildren);
    }

    private void createUserField(String name) {
        createCustomField(name, "userpicker", "userpickergroupsearcher");
    }

    private void createDateField(String name) {
        createCustomField(name, "datepicker", "daterange");
    }

    private CustomField createCustomField(String name, String type, String searcher) {
        log.trace("Trying to create custom field, name: {}, type: {}", name, type);
        CustomFieldManager customFieldManager = getCustomFieldManager();
        Collection<CustomField> customFieldObjectsByName = customFieldManager.getCustomFieldObjectsByName(name);
        CustomField result = null;
        String jiraType = "com.atlassian.jira.plugin.system.customfieldtypes:" + type;
        CustomFieldSearcher customFieldSearcher = customFieldManager.getCustomFieldSearcher("com.atlassian.jira.plugin.system.customfieldtypes:" + searcher);
        if (customFieldObjectsByName.isEmpty()) {
            try {
                result = customFieldManager.createCustomField(name, null,
                        customFieldManager.getCustomFieldType(jiraType),
                        customFieldSearcher,
                        buildJiraIssueContexts(true, null, getProjectManager()),
                        Arrays.asList((IssueType) null));
                log.debug("Successfully created custom field {}", name);
            } catch (GenericEntityException e) {
                e.printStackTrace();
            }
        } else {
            log.debug("Custom field with the name {} already exists", name);
            List<CustomField> customFields = customFieldObjectsByName.stream().sorted(Collections.reverseOrder()).collect(toList());
            customFields.stream().skip(1).forEach(customField -> {
                customFieldManager.updateCustomField(customField.getIdAsLong(), name + " OLD", null, customFieldSearcher);
                log.warn("!!!!RENAMED " + name + " to OLD");
            });
            result = customFields.get(0);
        }
        customFields.put(name, result);
        log.trace("Finished creation of custom field, name: {}, type: {}", name, type);
        return result;
    }

    private void createStatuses() {
        log.trace("Trying to create statuses");
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
        log.trace("Finished creation of statuses");
    }

    private void initStatusCategories() {
        StatusManager statusManager = getComponent(StatusManager.class);
        List<StatusCategoryName> categories = asList(StatusCategoryName.values());
        categories.forEach(statusCategory -> statusCategories.put(statusCategory, findStatusCategory(statusCategory.value)));
        statusCategories.forEach((key, value) -> statusCategoriesIcons.put(key, statusManager.getStatuses().stream().filter(status -> status.getStatusCategory().equals(value))
                .findFirst().get().getSimpleStatus().getIconUrl()));
    }

    private StatusCategory findStatusCategory(String name) {
        final String processedName = name.trim().replaceAll("_", " ");
        StatusCategoryManager manager = getComponent(StatusCategoryManager.class);
        return manager.getStatusCategories()
                .stream()
                .filter(category -> category.getName().equalsIgnoreCase(processedName)
                        || category.getKey().equalsIgnoreCase(processedName)
                        || category.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(processedName)))
                .findFirst().get();
    }

    private void createStatus(String name, StatusCategoryName category) {
        log.trace("Trying to create status {} from category {}", name, category.value);
        StatusService statusService = getComponent(StatusService.class);
        String iconUrl = statusCategoriesIcons.get(category);
        StatusCategory statusCategory = statusCategories.get(category);
        ServiceResult validateCreateStatus = statusService.validateCreateStatus(admin, name, "", iconUrl, statusCategory);
        if (validateCreateStatus.isValid()) {
            statusService.createStatus(admin, name, "", iconUrl, statusCategory);
        } else {
            logErrors(validateCreateStatus.getErrorCollection());
        }
        log.trace("Finished creation of status {} from category {}", name, category.value);
    }

    private void createScreens() {
        log.trace("Trying to create screens");
        createScreen(new IssueScreensFields(IMPROVEMENT).edit(SUMMARY, DUE_DATE, DESCRIPTION, LABELS, CONTRACTOR, ROOMER, PRIORITY, ACTUAL_COSTS).create(FUND_TYPE).view(PLANNED_COSTS));
        createFieldScreen("IMPROVEMENT Identify Scope", PLANNED_COSTS);
        createFieldScreen("IMPROVEMENT Work is Finished", ACTUAL_COSTS);

        createScreen(new IssueScreensFields(INCIDENT).edit(SUMMARY, DUE_DATE, DESCRIPTION, LABELS, ROOMER, PRIORITY, ACTUAL_COSTS).create(FUND_TYPE).view(CONTRACTOR));
        createFieldScreen("INCIDENT Identify Scope", CONTRACTOR);
        createFieldScreen("INCIDENT Work is Finished", ACTUAL_COSTS);

        createScreen(new IssueScreensFields(TASK).edit(SUMMARY, DUE_DATE, DESCRIPTION, LABELS).create(PLANNED_COSTS).view(ACTUAL_COSTS, CONTRACTOR));
        createFieldScreen("TASK Start Progress", ACTUAL_COSTS, CONTRACTOR);

        createScreen(new IssueScreensFields(PAYMENT).edit(SUMMARY, DUE_DATE, DESCRIPTION, LABELS).create(PLANNED_COSTS, FUND_TYPE, FUND_COLLECTION_MANNER).view(ACTUAL_COSTS));
        createFieldScreen("PAYMENT Resolve", ACTUAL_COSTS);

        createScreen(new IssueScreensFields(PAYMENT_NOTIFY).edit(SUMMARY, DESCRIPTION, LABELS, ACTUAL_COSTS).create(PLANNED_COSTS, ROOMER, ASSIGNEE).view());
        createFieldScreen("PAYMENT NOTIFY Confirm", ACTUAL_COSTS);

        createScreen(new IssueScreensFields(PUBLIC).edit(SUMMARY, DUE_DATE, DESCRIPTION, LABELS, ACT_START_DATE, ACT_END_DATE, PRIORITY));

        createScreen(new IssueScreensFields(BOARDING).create(SUMMARY, DUE_DATE, DESCRIPTION, LABELS, CONTRACTOR_NAME, CONTRACTOR_ID, ACCOUNT_NUMBER, CONTRACTOR_BANK, CONTRACTOR_BANK_ID, BUSINESS_ADDRESS, PHONE, BUSINESS_MAIL, EDRPOU));
        createScreen(new IssueScreensFields(BOARDING_VALIDATION).edit(SUMMARY, DUE_DATE, DESCRIPTION, ASSIGNEE));
        log.trace("Finished creation of screens");
    }

    private void createScreen(String issueTypeName, ScreenableIssueOperation operation, String... fields) {
        log.trace("Trying to create screen for issue type {} and operation {}", issueTypeName, operation.getNameKey());
        log.trace("Fields: {}", Arrays.toString(fields));
        IssueTypeScreenSchemeManager issueTypeScreenSchemeManager = getIssueTypeScreenSchemeManager();
        FieldScreenSchemeManager fieldScreenSchemeManager = getComponent(FieldScreenSchemeManager.class);
        FieldScreenManager fieldScreenManager = getFieldScreenManager();

        IssueTypeScreenScheme projectScreenScheme = issueTypeScreenSchemeManager.getIssueTypeScreenScheme(project); //NSMD Issue type screen scheme

        IssueType issueType = issueTypes.get(issueTypeName);
        FieldScreenScheme issueOperations = projectScreenScheme.getEffectiveFieldScreenScheme(issueType); //Improvement operations
        String targetFieldScreenName = issueTypeName + " operations";

        if (!issueOperations.getName().equals(targetFieldScreenName)) {
            issueOperations = new FieldScreenSchemeImpl(fieldScreenSchemeManager);
            issueOperations.setName(targetFieldScreenName);
            issueOperations.store();

            IssueTypeScreenSchemeEntityImpl issueTypeScreenSchemeEntity = new IssueTypeScreenSchemeEntityImpl(issueTypeScreenSchemeManager, fieldScreenSchemeManager, getConstantsManager());
            issueTypeScreenSchemeEntity.setFieldScreenScheme(issueOperations);
            issueTypeScreenSchemeEntity.setIssueTypeId(issueType.getId());
            projectScreenScheme.addEntity(issueTypeScreenSchemeEntity);
        }

        FieldScreenSchemeItem issueOperation = issueOperations.getFieldScreenSchemeItem(operation);

        if (issueOperation == null) {
            issueOperation = new FieldScreenSchemeItemImpl(fieldScreenSchemeManager, fieldScreenManager);
            issueOperation.setIssueOperation(operation);
        }

        String screenName = issueTypeName + " " + getCapitalizedName(operation);
        issueOperation.setFieldScreen(createFieldScreen(screenName, fields));

        issueOperations.addFieldScreenSchemeItem(issueOperation);
        issueOperation.store();

        log.trace("Finished creation of screen for issue type {} and operation {}", issueTypeName, operation.getNameKey());
    }

    private void createScreen(IssueScreensFields issueScreensFields) {
        createScreen(issueScreensFields.issueType, issueScreensFields.fieldsMap);
    }

    private void createScreen(String issueType, ScreenableIssueOperation operation, Collection<String> fields) {
        createScreen(issueType, operation, fields.toArray(new String[fields.size()]));
    }

    private void createScreen(String issueType, Map<String, FieldAvailability> fields) {
        asList(FieldAvailability.values()).forEach(fieldAvailability -> createScreen(issueType, fieldAvailability.getOperation(), fields.entrySet().stream()
                .filter(entry -> entry.getValue().ordinal() >= fieldAvailability.ordinal()).map(Map.Entry::getKey).collect(toList())));
    }

    private FieldScreen createFieldScreen(String name, String... fields) {
        log.trace("Trying to create screen {}, with fields: {}", name, Arrays.toString(fields));
        FieldScreen fieldScreen = getFieldScreenManager().getFieldScreens().stream().filter(screen -> screen.getName().equals(name)).findAny().orElseGet(() -> {
            FieldScreen screen = new FieldScreenImpl(getFieldScreenManager());
            screen.setName(name);
            screen.addTab("TAB");
            screen.store();
            return screen;
        });
        FieldScreenTab tab = fieldScreen.getTab(0);
        for (int i = 0; i < fields.length; i++) {
            tab.addFieldScreenLayoutItem(findField(fields[i]).getId(), i);
        }
        tab.getFieldScreenLayoutItems().stream().skip(fields.length).forEach(FieldScreenLayoutItem::remove);
        screensIds.put(CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name.toUpperCase().replace(" ", "_")), fieldScreen.getId());
        log.trace("Finished creation of screen {}, with fields: {}", name, Arrays.toString(fields));
        return fieldScreen;
    }

    private void createWorkflows() {
        createWorkflow(IMPROVEMENT);
        createWorkflow(INCIDENT);
        createWorkflow(TASK);
        createWorkflow(BOARDING_VALIDATION);
        createWorkflow(PAYMENT);
        createWorkflow(PAYMENT_NOTIFY);
    }

    private void createWorkflow(String issueTypeName) {
        try {
            WorkflowSchemeManager workflowSchemeManager = getWorkflowSchemeManager();
            String workflowSchemeName = "NSMD Workflow: Default schema";
            workflowSchemeManager.getSchemeObject(workflowSchemeName);
            AssignableWorkflowScheme workflowScheme = workflowSchemeManager.getWorkflowSchemeObj(project); // necessary intermediate step
            if (!workflowScheme.getName().equals(workflowSchemeName)) {
                workflowSchemeManager.removeSchemesFromProject(project);
                workflowSchemeManager.addSchemeToProject(project, workflowSchemeManager.createSchemeObject(workflowSchemeName, ""));
                workflowScheme = workflowSchemeManager.getWorkflowSchemeObj(project);
            }
            AssignableWorkflowScheme.Builder workflowSchemeBuilder = workflowScheme.builder();
            String workflowName = importWorkflow(issueTypeName, workflowScheme).getName();
            workflowSchemeBuilder.setMapping(issueTypes.get(issueTypeName).getId(), workflowName);
            workflowSchemeManager.updateWorkflowScheme(workflowSchemeBuilder.build());
        } catch (Exception e) {
            log.error("Exception while creating workflow " + issueTypeName, e);
        }
    }

    private JiraWorkflow importWorkflow(String issueType, AssignableWorkflowScheme workflowScheme) {
        try {
            String issueTypeEdited = issueType.replace(" ", "");
            WorkflowManager workflowManager = getWorkflowManager();
            String name = issueTypeEdited + "WF";
            JiraWorkflow existingWorkflow = workflowManager.getWorkflow(name);
            if (existingWorkflow != null) {
                AssignableWorkflowScheme.Builder builder = workflowScheme.builder();
                builder.removeMapping(issueTypes.get(issueType).getId());
                getWorkflowSchemeManager().updateWorkflowScheme(builder.build());
                workflowManager.deleteWorkflow(existingWorkflow);
            }
            final WorkflowDescriptor workflowDescriptor = WorkflowLoader.load(templateEngine.renderAsStream("workflows/" + issueTypeEdited + ".xml", screensIds), true);
            ConfigurableJiraWorkflow newWorkflow = new ConfigurableJiraWorkflow(name, workflowDescriptor, workflowManager);
            workflowManager.createWorkflow(admin, newWorkflow);
            return newWorkflow;
        } catch (Exception e) {
            log.error("Exception while importing workflow " + issueType, e);
            return null;
        }
    }

    private Field findField(String name) {
        try {
            return getFieldManager().getAllAvailableNavigableFields().stream().filter(field -> field.getName().equalsIgnoreCase(name)).findFirst().get();
        } catch (FieldException e) {
            log.error("Failed to find field {}", name);
            throw new RuntimeException(e);
        }
    }

    private String getCapitalizedName(ScreenableIssueOperation operation) {
        String[] strings = operation.getNameKey().split("\\.");
        return strings[strings.length - 1].toUpperCase();
    }

    private void logErrors(ErrorCollection errors) {
        if (errors.hasAnyErrors()) {
            if (errors.getErrors().size() > 0) {
                log.warn(Arrays.toString(errors.getErrors().values().toArray()));
            }
            if (errors.getErrorMessages().size() > 0) {
                log.warn(Arrays.toString(errors.getErrorMessages().toArray()));
            }
        }
    }

    private static class IssueScreensFields {

        private String issueType;
        private Map<String, FieldAvailability> fieldsMap = new LinkedHashMap<>();

        public IssueScreensFields(String issueType) {
            this.issueType = issueType;
        }

        public IssueScreensFields edit(String... fields) {
            Arrays.asList(fields).forEach(field -> this.fieldsMap.put(field, EDIT));
            return this;
        }

        public IssueScreensFields create(String... fields) {
            Arrays.asList(fields).forEach(field -> fieldsMap.put(field, CREATE));
            return this;
        }

        public IssueScreensFields view(String... fields) {
            Arrays.asList(fields).forEach(field -> fieldsMap.put(field, VIEW));
            return this;
        }

    }
}
