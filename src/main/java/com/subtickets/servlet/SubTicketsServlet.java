package com.subtickets.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.project.ProjectCreationData;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.IssueTypeService;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserDetails;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.sal.api.net.ResponseException;
import com.subtickets.roomers.Roomer;
import com.subtickets.roomers.Roomers;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.subtickets.Constants.ACTUAL_COSTS_FIELD_NAME;
import static com.subtickets.Constants.AUTO_COMPONENT_NAME;
import static com.subtickets.Constants.CREATE_SUBS_URL;
import static com.subtickets.Constants.DOOR_COMPONENT_NAME;
import static com.subtickets.Constants.FUND_COLLECTION_MANNER_FIELD_NAME;
import static com.subtickets.Constants.FUND_TYPE_FIELD_NAME;
import static com.subtickets.Constants.PAYMENT_ISSUE_TYPE_NAME;
import static com.subtickets.Constants.PAYMENT_SUB_ISSUE_TYPE_NAME;
import static com.subtickets.Constants.PLANNED_COSTS_FIELD_NAME;
import static com.subtickets.Constants.ROOMERS_URL;
import static com.subtickets.Constants.SQUARE_COMPONENT_NAME;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

@Named
public class SubTicketsServlet extends HttpServlet {

    private static CustomField PLANNED_COSTS_FIELD;
    private static CustomField ACTUAL_COSTS_FIELD;
    private static CustomField FUND_TYPE_FIELD;
    private static CustomField FUND_COLLECTION_MANNER_TYPE_FIELD;

    private IssueType fundPaymentIssueType;
    private IssueType fundPaymentSubIssueType;

    private ApplicationUser roomerUser;
    private ApplicationUser admin;

    private IssueLinkType blockedBy;

    @ComponentImport
    private IssueService issueService;

    @ComponentImport
    private com.atlassian.sal.api.user.UserManager userManager;

    @ComponentImport
    private RequestFactory<?> requestFactory;

    @ComponentImport
    private LabelManager labelManager;

    @ComponentImport
    private SubTaskManager subTaskManager;

    private UserManager jiraUserManager;

    private IssueLinkManager issueLinkManager;

    @Inject
    public SubTicketsServlet(IssueService issueService, com.atlassian.sal.api.user.UserManager userManager, RequestFactory requestFactory, LabelManager labelManager,
                             SubTaskManager subTaskManager) {
        this.issueService = issueService;
        this.userManager = userManager;
        this.jiraUserManager = ComponentAccessor.getUserManager();
        this.requestFactory = requestFactory;
        this.labelManager = labelManager;
        this.subTaskManager = subTaskManager;
        issueLinkManager = ComponentAccessor.getIssueLinkManager();

        blockedBy = ComponentAccessor.getComponent(IssueLinkTypeManager.class)
                .getIssueLinkTypesByInwardDescription(BLOCKED_BY_LINK_TYPE_NAME).stream().findFirst().get();

        if (this.jiraUserManager.getUserByName("Roomer") != null) {
            roomerUser = this.jiraUserManager.getUserByName("Roomer");
        } else if (this.jiraUserManager.getUserByName("roomer") != null) {
            roomerUser = this.jiraUserManager.getUserByName("roomer");
        } else {
            try {
                roomerUser = this.jiraUserManager.createUser(new UserDetails("Roomer", "Roomer"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        admin = jiraUserManager.getUserByName("admin");

        bootstrap();

        ComponentAccessor.getCustomFieldManager().getCustomFieldObjects().forEach(customField -> {
            switch (customField.getUntranslatedName()) {
                case ACTUAL_COSTS_FIELD_NAME:
                    ACTUAL_COSTS_FIELD = customField;
                    break;
                case PLANNED_COSTS_FIELD_NAME:
                    PLANNED_COSTS_FIELD = customField;
                    break;
                case FUND_TYPE_FIELD_NAME:
                    FUND_TYPE_FIELD = customField;
                    break;
                case FUND_COLLECTION_MANNER_FIELD_NAME:
                    FUND_COLLECTION_MANNER_TYPE_FIELD = customField;
                    break;
            }
        });

        Collection<IssueType> issueTypes = ComponentAccessor.getConstantsManager().getAllIssueTypeObjects();
        issueTypes.forEach(type -> {
            switch (type.getName()) {
                case (PAYMENT_ISSUE_TYPE_NAME):
                    fundPaymentIssueType = type;
                    break;
                case (PAYMENT_SUB_ISSUE_TYPE_NAME):
                    fundPaymentSubIssueType = type;
                    break;
            }
        });
    }

    private void bootstrap() {
        createProject();
        createIssueTypes();
        initDBTable();
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
                System.out.println("Table cwd_user_pass exists");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stmt.close();
                conn.close();
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
                .withName("New OSMD")
                .withKey("NSMD")
                .withType("business")
                .withLead(admin)
                .withAssigneeType(AssigneeTypes.UNASSIGNED)
                .build());
        if (createProjectValidationResult.isValid()) {
            projectService.createProject(createProjectValidationResult);
        } else {
            System.err.println(Arrays.toString(createProjectValidationResult.getErrorCollection().getErrors().entrySet().toArray()));
        }
    }

    private void createIssueTypes() {
        createIssueType("Improvement");
        createIssueType("Incident");
        createIssueType("Notification");
        createIssueType("Payment");
        createIssueType("Task");
        createIssueType("Voting");
        createIssueType("Voting session");
        createIssueType("TEST");
        createSubIssueType("Payment Notify");
        createSubIssueType("Sub-task");
        createSubIssueType("Voting Notify");
        createSubIssueType("Test Notify");
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
        if (createValidationResult.isValid()) {
            issueTypeService.createIssueType(admin, createValidationResult);
        } else {
            System.err.println(Arrays.toString(createValidationResult.getErrorCollection().getErrors().entrySet().toArray()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ApplicationUser applicationUser = jiraUserManager.getUserByKey(userManager.getRemoteUser(req).getUserKey().getStringValue());
        String issueId = req.getParameter("id");
        MutableIssue issue = issueService.getIssue(applicationUser, issueId).getIssue();

        try {
//            String fundType = getFundType(issue);
//            Predicate<String> candidateMatch = getCandidateMatch(fundType);
//            if (ESTEBLISHED_FUND_TYPE_VALUE.stream().anyMatch(candidateMatch)) {
//                createMonthlyPaymentsSubIssues(applicationUser, issue);
//            } else if (CUSTOM_FUND_TYPE_VALUE.stream().anyMatch(candidateMatch)) {
//                createFundPaymentSubIssues(applicationUser, issue);
//            }
            Request<?, ?> request = requestFactory.createRequest(Request.MethodType.POST, CREATE_SUBS_URL + "?id=" + issueId);
            request.setSoTimeout(200000);
            String execute = request.execute();
            System.out.println(execute);
        } catch (ResponseException e) {
            e.printStackTrace();
        }

        resp.sendRedirect(req.getHeader("referer"));
    }

    private void createMonthlyPaymentsSubIssues(ApplicationUser user, Issue issue) throws ResponseException {
        getRoomers().forEach((key, roomer) -> {
            IssueInputParameters parameters = generateIssueInputParameters(user, issue)
                    .setSummary(issue.getSummary() + " - " + roomer.holderInfo);
            Issue subIssue = doCreateSubIssue(user, issue, parameters);
            if (subIssue != null) {
                setPlannedCosts(subIssue, roomer.amount);
                addLabels(user, subIssue, roomer.owned_Doors);
            }
        });
    }

    private void createFundPaymentSubIssues(ApplicationUser user, Issue issue) throws ResponseException {
        String collectionManner = getCollectionManner(issue);
        Predicate<String> candidateMatch = getCandidateMatch(collectionManner);
        if (AUTO_COMPONENT_NAME.stream().anyMatch(candidateMatch)) {
            createAutoFundPayments(user, issue);
        } else if (DOOR_COMPONENT_NAME.stream().anyMatch(candidateMatch)) {
            createDoorFundPayments(user, issue);
        } else if (SQUARE_COMPONENT_NAME.stream().anyMatch(candidateMatch)) {
            createSquareFundPayment(user, issue);
        }
    }

    private void createAutoFundPayments(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getAutosToCount, Roomer::getAutos);
    }

    private void createDoorFundPayments(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getDoorsToCount, Roomer::getDoors);
    }

    private void createSquareFundPayment(ApplicationUser user, Issue issue) throws ResponseException {
        createFundPayments(user, issue, Roomer::getSquareToCount, null);
    }

    private void createFundPayments(ApplicationUser user, Issue issue, Function<Roomer, Number> items, Function<Roomer, String[]> labels) throws ResponseException {
        Roomers roomers = getRoomers();
        Double itemsCount = roomers.values().stream()
                .map(items)
                .mapToDouble(Number::doubleValue)
                .sum();
        Double plannedCosts = getPlannedCosts(issue);
        Double singlePayment = (Math.ceil((plannedCosts / itemsCount) * 100)) / 100.0;
        roomers.values().stream()
                .filter(roomer -> items.apply(roomer).doubleValue() > 0)
                .forEach(roomer -> {
                    IssueInputParameters parameters = generateIssueInputParameters(user, issue)
                            .setSummary(issue.getSummary() + " - " + roomer.fio);
                    Issue subIssue = doCreateSubIssue(user, issue, parameters);
                    if (subIssue != null) {
                        setPlannedCosts(subIssue, singlePayment * items.apply(roomer).doubleValue());
                        String[] derivedLabels = labels != null ? labels.apply(roomer) : new String[]{};
                        addLabels(user, subIssue, Stream.concat(Arrays.stream(derivedLabels), Stream.of(getInitials(roomer.fio))).collect(toSet()));
                    }
                });
    }

    private IssueInputParameters generateIssueInputParameters(ApplicationUser user, Issue issue) {
        return new IssueInputParametersImpl()
                .setAssigneeId(roomerUser.getName())
                .setReporterId(user.getName())
                .setProjectId(issue.getProjectId())
                .setIssueTypeId(fundPaymentSubIssueType.getId());
    }

    private Issue doCreateSubIssue(ApplicationUser user, Issue parentIssue, IssueInputParameters parameters) {
        IssueService.CreateValidationResult validationResult = issueService.validateSubTaskCreate(user, parentIssue.getId(), parameters);
        if (validationResult.isValid()) {
            IssueService.IssueResult issueResult = issueService.create(user, validationResult);
            MutableIssue subIssue = issueResult.getIssue();
            try {
                subTaskManager.createSubTaskIssueLink(parentIssue, subIssue, user);
                issueLinkManager.createIssueLink(subIssue.getId(), parentIssue.getId(), blockedBy.getId(), 0L, user);
            } catch (CreateException e) {
                e.printStackTrace();
            }
            return subIssue;
        }
        return null;
    }

    private String getCollectionManner(Issue issue) {
        return issue.getCustomFieldValue(FUND_COLLECTION_MANNER_TYPE_FIELD).toString();
    }

    private void setActualCosts(Issue issue, Double value) {
        ModifiedValue modifiedValue = new ModifiedValue<>(0.0d, value);
        PLANNED_COSTS_FIELD.updateValue(null, issue, modifiedValue, new DefaultIssueChangeHolder());
    }

    private void setPlannedCosts(Issue issue, Double value) {
        ModifiedValue modifiedValue = new ModifiedValue<>(0.0d, value);
        PLANNED_COSTS_FIELD.updateValue(null, issue, modifiedValue, new DefaultIssueChangeHolder());
    }

    private Double getPlannedCosts(Issue issue) {
        return (Double) issue.getCustomFieldValue(PLANNED_COSTS_FIELD);
    }

    private String getFundType(Issue issue) {
        return issue.getCustomFieldValue(FUND_TYPE_FIELD).toString();
    }

    private String getInitials(String fullName) {
        String[] names = fullName.split(" ");
        String initials = Arrays.stream(names, 1, names.length)
                .map(name -> String.valueOf(name.charAt(0)))
                .collect(joining("."));
        return names[0] + "_" + initials + ".";
    }

    private Roomers getRoomers() throws ResponseException {
        return requestFactory.createRequest(Request.MethodType.GET, ROOMERS_URL).executeAndReturn(new RoomersResponseHandler());
    }

    private void addLabels(ApplicationUser user, Issue issue, Collection<String> labels) {
        Set<String> labelsSet = labels.stream().map(label -> label.replaceAll(" ", "_")).collect(toSet());
        labelManager.setLabels(user, issue.getId(), labelsSet, true, true);
    }

    private void addLabels(ApplicationUser user, Issue issue, String... labels) {
        addLabels(user, issue, Stream.of(labels).collect(toSet()));
    }

    private Predicate<String> getCandidateMatch(String target) {
        return candidate -> candidate.equals(target);
    }
}