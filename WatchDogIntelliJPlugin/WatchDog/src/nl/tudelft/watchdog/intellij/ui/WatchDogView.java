package nl.tudelft.watchdog.intellij.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import nl.tudelft.watchdog.core.logic.interval.IntervalStatisticsBase.StatisticsTimePeriod;
import nl.tudelft.watchdog.core.logic.interval.intervaltypes.DebugInterval;
import nl.tudelft.watchdog.core.ui.util.DebugEventVisualizationUtils;
import nl.tudelft.watchdog.intellij.logic.InitializationManager;
import nl.tudelft.watchdog.intellij.logic.event.EventStatistics;
import nl.tudelft.watchdog.intellij.logic.interval.IntervalStatistics;
import nl.tudelft.watchdog.intellij.ui.util.UIUtils;
import nl.tudelft.watchdog.intellij.util.WatchDogUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.GanttRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.gantt.GanttCategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.util.Rotation;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * A view displaying all the statistics that WatchDog has gathered.
 */
public class WatchDogView extends SimpleToolWindowPanel {
    private static final float FOREGROUND_TRANSPARENCY = 0.8f;

    /**
     * The Id of the view.
     */
    public static final String ID = "WatchDog.view";

    private IntervalStatistics intervalStatistics;
    private EventStatistics eventStatistics;

    private JComponent parent = getComponent();

    private double intelliJOpen;
    private double userActive;
    private double userReading;
    private double userTyping;
    private double userProduction;
    private double userTest;
    private double userActiveRest;
    private double averageTestDurationMinutes;
    private double averageTestDurationSeconds;

    private int junitRunsCount;
    private int junitFailuresCount;
    private int junitSuccessCount;

    private int debuggingSessionCount;
    private double totalDebuggingTime;
    private double debuggingTimePercentage;
    private double averageDebuggingTime;

    private StatisticsTimePeriod selectedTimePeriod = StatisticsTimePeriod.HOUR_1;

    private DebugInterval selectedDebugInterval;
    private List<DebugInterval> latestDebugIntervals;
    private static final int NUMBER_OF_INTERVALS_TO_SHOW = 10;

    private JPanel oneColumn;
    private JPanel intervalSelection;
    private ComboBox intervalSelectionBox;
    private ComboBox debugIntervalSelectionBox;


    public WatchDogView(boolean vertical) {
        super(vertical);
        createWatchDogView();
    }

    /**
     * Updates the view by completely repainting it.
     */
    public void update() {
        parent.removeAll();
        createWatchDogView();
        parent.updateUI();
    }


    public void createWatchDogView() {
        oneColumn = UIUtils.createVerticalBoxJPanel(parent);

        if (!WatchDogUtils.isWatchDogActive(WatchDogUtils.getProject())) {
            createInactiveViewContent();
        } else {
            calculateTimes();
            latestDebugIntervals = intervalStatistics.getLatestDebugIntervals(NUMBER_OF_INTERVALS_TO_SHOW);
            if (selectedDebugIntervalShouldBeReset()) {
                selectedDebugInterval = !latestDebugIntervals.isEmpty()? latestDebugIntervals.get(0) : null;
            }
            createActiveView();
            makeScrollable();
        }
        // Always create refresh link, even when statistics are not shown
        createRefreshLink(intervalSelection);
    }

    /**
     * @return true if and only if one of the following two conditions hold:
     *
     * 1. No debug interval has been selected yet; or 2. A debug
     * interval has been selected before, but it is no longer part of
     * the latest debug intervals.
     */
    private boolean selectedDebugIntervalShouldBeReset() {
        return (selectedDebugInterval == null
                || !latestDebugIntervals.contains(selectedDebugInterval));
    }

    private void makeScrollable() {
        JBScrollPane scrollPane = new JBScrollPane(oneColumn);
        parent.add(scrollPane);
        scrollPane.setViewportView(oneColumn);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    }

    private void createInactiveViewContent() {
        JComponent container = UIUtils.createVerticalBoxJPanel(oneColumn);
        UIUtils.createBoldLabel(container, "<html>WatchDog is not active in this workspace! <br>");
        UIUtils.createLabel(container,
                "<html>Therefore we cannot show you any cool test statistics. <br>To get them, go to settings and enable WatchDog.");
        intervalSelection = UIUtils.createFlowJPanelLeft(oneColumn);
    }

    private void createActiveView() {
        // General section.
        UIUtils.createTitleLabel(UIUtils.createGridedJPanel(oneColumn, 1), "General\n");

        JComponent generalSectionContainer = UIUtils.createGridedJPanel(oneColumn, 2);

        createChartPanel(
                generalSectionContainer,
                createBarChart(createDevelopmentBarDataset(),
                        "Your Development Activity", "", "minutes"));
        createChartPanel(
                generalSectionContainer,
                createPieChart(createDevelopmentPieDataset(),
                        "Your Development Activity"));

        // Testing section.
        UIUtils.createTitleLabel(UIUtils.createGridedJPanel(oneColumn, 1), "Testing\n");
        JComponent testingSectionContainer = UIUtils.createGridedJPanel(oneColumn, 2);

        createChartPanel(
                testingSectionContainer,
                createBarChart(createProductionVSTestBarDataset(),
                        "Your Production vs. Test Activity", "", "minutes"));
        createChartPanel(
                testingSectionContainer,
                createPieChart(createProductionVSTestPieDataset(),
                        "Your Production vs. Test Activity"));
        createChartPanel(
                testingSectionContainer,
                createStackedBarChart(createJunitExecutionBarDataset(),
                        "Your Test Run Activity", "", ""));

        // Debugging section.
        if (selectedDebugInterval != null) {
            UIUtils.createTitleLabel(UIUtils.createGridedJPanel(oneColumn, 1), "Debugging\n");
            JComponent debugSectionContainer = UIUtils.createGridedJPanel(oneColumn, 2);
            createChartPanel(debugSectionContainer, createDebugEventGanttChart());
            createDebugStatisticsLabels(UIUtils.createGridedJPanel(debugSectionContainer, 1));
        }

        // Controls.
        createShowingStatisticsLines();
        createTimeSpanSelectionList();
        UIUtils.createStartDebugSurveyLink(oneColumn);
    }

    private void createDebugStatisticsLabels(JPanel container) {
        UIUtils.createLabel(container, "Number of debugging intervals in the selected period: " + debuggingSessionCount);
        UIUtils.createLabel(container, String.format("Time spent in debugger: %.2f minutes (%.2f%% of active IDE time)",
                totalDebuggingTime, debuggingTimePercentage));
        UIUtils.createLabel(container,
                String.format("Average debugging session length: %.2f seconds",
                        60 * averageDebuggingTime));
        createDebugIntervalSelectionList(container);
    }

    private JFreeChart createDebugEventGanttChart() {
        eventStatistics = new EventStatistics(
                InitializationManager.getInstance(WatchDogUtils.getProject()).getDebugEventManager(),
                selectedDebugInterval);
        GanttCategoryDataset dataset = eventStatistics.createDebugEventGanttChartDataset();

        JFreeChart chart = ChartFactory.createGanttChart(
                "Debug Events During Selected Debug Interval", "Event", "Time", dataset, false, true, false);

        // Scale the chart based on the selected debug interval.
        CategoryPlot plot = chart.getCategoryPlot();
        ValueAxis axis = plot.getRangeAxis();
        axis.setRangeWithMargins(selectedDebugInterval.getStart().getTime() - EventStatistics.PRE_SESSION_TIME_TO_INCLUDE,
                selectedDebugInterval.getEnd().getTime());

        // Give each event type a different color.
        plot.setRenderer(new WatchDogGanttRenderer());
        return chart;
    }

    private class WatchDogGanttRenderer extends GanttRenderer {

        private static final long serialVersionUID = 1L;

        public WatchDogGanttRenderer() {
            super();
            this.setShadowVisible(false);
        }

        public Paint getItemPaint(int row, int column) {
            return DebugEventVisualizationUtils.getColorForNumber(column);
        }
    }


    private void createShowingStatisticsLines() {
        JPanel lines = UIUtils.createGridedJPanel(oneColumn, 1);
        UIUtils.createLabel(lines,
                "Showing statistics from " + intervalStatistics.oldestDate
                        + " to " + intervalStatistics.mostRecentDate + " ("
                        + intervalStatistics.getNumberOfIntervals()
                        + " intervals).");
        JPanel reportLine = UIUtils.createFlowJPanelLeft(lines);
        UIUtils.createLabel(reportLine, "Not enough statistics for you? ");
        UIUtils.createOpenReportLink(reportLine);
    }

    private void createTimeSpanSelectionList() {
        intervalSelection = UIUtils.createFlowJPanelLeft(oneColumn);
        UIUtils.createLabel(intervalSelection, "Show statistics of the past ");

        intervalSelectionBox = UIUtils.createComboBox(intervalSelection, new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {

                selectedTimePeriod = StatisticsTimePeriod.values()[intervalSelectionBox.getSelectedIndex()];
            }
        }, StatisticsTimePeriod.names(), selectedTimePeriod.ordinal());
    }

    private void createDebugIntervalSelectionList(JComponent parent) {
        JPanel debugLine = UIUtils.createGridedJPanel(parent, 1);
        UIUtils.createLabel(debugLine, "");
        JPanel debugIntervalSelection = UIUtils.createFlowJPanelLeft(debugLine);
        UIUtils.createLabel(debugIntervalSelection, "Show debug events for debug interval ");

        debugIntervalSelectionBox = UIUtils.createComboBox(debugIntervalSelection, new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                selectedDebugInterval = latestDebugIntervals.get(debugIntervalSelectionBox.getSelectedIndex());
                update();
            }
        }, DebugEventVisualizationUtils.getDebugIntervalStrings(latestDebugIntervals), latestDebugIntervals.indexOf(selectedDebugInterval));
        debugIntervalSelectionBox.setMinimumAndPreferredWidth(300);
    }

    private void createRefreshLink(JComponent parent) {
        UIUtils.createButton(parent, "Refresh.", new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                update();
            }
        });
    }

    private void calculateTimes() {
        intervalStatistics = new IntervalStatistics(InitializationManager
                .getInstance(WatchDogUtils.getProject()).getIntervalManager(), selectedTimePeriod);

        intelliJOpen = intervalStatistics
                .getPreciseTime(intervalStatistics.ideOpen);
        userActive = intervalStatistics
                .getPreciseTime(intervalStatistics.userActive);
        userReading = intervalStatistics
                .getPreciseTime(intervalStatistics.userReading);
        userTyping = intervalStatistics
                .getPreciseTime(intervalStatistics.userTyping);
        userProduction = intervalStatistics
                .getPreciseTime(intervalStatistics.userProduction);
        userTest = intervalStatistics
                .getPreciseTime(intervalStatistics.userTest);
        userActiveRest = userActive - userReading - userTyping;
        averageTestDurationMinutes = intervalStatistics.averageTestDuration;
        averageTestDurationSeconds = averageTestDurationMinutes * 60;

        junitRunsCount = intervalStatistics.junitRunsCount;
        junitSuccessCount = intervalStatistics.junitSuccessfulRunsCount;
        junitFailuresCount = intervalStatistics.junitFailedRunsCount;

        debuggingSessionCount = intervalStatistics.debuggingSessionCount;
        totalDebuggingTime = intervalStatistics
                .getPreciseTime(intervalStatistics.totalDebuggingDuration);
        debuggingTimePercentage = totalDebuggingTime / userActive;
        averageDebuggingTime = intervalStatistics
                .getPreciseTime(intervalStatistics.averageDebuggingDuration);
    }

    private void createChartPanel(JComponent parent, JFreeChart chart) {
        ChartPanel chartPanel = new ChartPanel(chart);
        parent.add(chartPanel);
    }

    private DefaultCategoryDataset createDevelopmentBarDataset() {
        DefaultCategoryDataset result = new DefaultCategoryDataset();
        result.setValue(userReading, "1", "Reading");
        result.setValue(userTyping, "1", "Writing");
        result.setValue(userActive, "1", "User Active");
        result.setValue(intelliJOpen, "1", "IntelliJ Open");
        return result;
    }

    private PieDataset createDevelopmentPieDataset() {
        double divisor = userReading + userTyping + userActiveRest;
        DefaultPieDataset result = new DefaultPieDataset();
        result.setValue("Reading" + printPercent(userReading, divisor),
                userReading);
        result.setValue("Writing" + printPercent(userTyping, divisor),
                userTyping);
        result.setValue(
                "Other activities" + printPercent(userActiveRest, divisor),
                userActiveRest);
        return result;
    }

    private DefaultCategoryDataset createProductionVSTestBarDataset() {
        DefaultCategoryDataset result = new DefaultCategoryDataset();
        result.setValue(userProduction, "1", "Production Code");
        result.setValue(userTest, "1", "Test Code");
        return result;
    }

    private PieDataset createProductionVSTestPieDataset() {
        double divisor = userProduction + userTest;
        DefaultPieDataset result = new DefaultPieDataset();
        result.setValue(
                "Production Code" + printPercent(userProduction, divisor),
                userProduction);
        result.setValue("Test Code" + printPercent(userTest, divisor), userTest);
        return result;
    }

    private JFreeChart createPieChart(final PieDataset dataset, String title) {
        JFreeChart chart = ChartFactory.createPieChart3D(title, dataset, true,
                true, false);
        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setForegroundAlpha(FOREGROUND_TRANSPARENCY);
        return chart;
    }

    private JFreeChart createBarChart(final DefaultCategoryDataset dataset,
                                      String title, String xAxisName, String yAxisName) {
        JFreeChart chart = ChartFactory.createBarChart3D(title, xAxisName,
                yAxisName, dataset);
        chart.getLegend().setVisible(false);
        return chart;
    }

    private JFreeChart createStackedBarChart(CategoryDataset dataset,
                                             String title, String xAxisName, String yAxisName) {
        JFreeChart chart = ChartFactory.createStackedBarChart3D(title,
                xAxisName, yAxisName, dataset);
        chart.getLegend().setVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        CategoryItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, makeColorTransparent(JBColor.GREEN));
        renderer.setSeriesPaint(1, makeColorTransparent(JBColor.RED));
        renderer.setSeriesPaint(2, makeColorTransparent(JBColor.BLUE));
        return chart;
    }

    /**
     * Takes a color and modifies its alpha channel to give it (roughly) the
     * same transparency level that other JFreeCharts have per default, or that
     * can be set using {@link Plot#setForegroundAlpha(float)}. Workaround for
     * StackedBarCharts, where the above mentioned does not work.
     */
    private JBColor makeColorTransparent(JBColor color) {
        int adjustedTransparency = (int) Math
                .round(FOREGROUND_TRANSPARENCY * 0.6 * 255);
        return new JBColor(color.getRGB(), adjustedTransparency);
    }


    private CategoryDataset createJunitExecutionBarDataset() {
        double differenceSeconds = Math.abs(averageTestDurationSeconds
                - junitRunsCount);
        double differenceMinutes = Math.abs(averageTestDurationMinutes
                - junitRunsCount);

        String testDurationTitle = "Test Run Duration";
        double testDuration;
        if (differenceSeconds < differenceMinutes) {
            testDuration = averageTestDurationSeconds;
            testDurationTitle += " (in seconds)";
        } else {
            testDuration = averageTestDurationMinutes;
            testDurationTitle += " (in minutes)";
        }

        String[] columns = new String[]{"Successful", "Failed", "Both"};
        String[] rows = new String[]{"Test Runs", testDurationTitle};
        double[][] data = new double[][]{{junitSuccessCount, 0},
                {junitFailuresCount, 0}, {0, testDuration}};
        CategoryDataset dataSet = DatasetUtilities.createCategoryDataset(
                columns, rows, data);

        return dataSet;
    }

    private String printPercent(double dividend, double divisor) {
        if (divisor == 0) {
            return " (--)";
        }
        return " (" + String.format("%.1f", dividend * 100 / divisor) + "%)";
    }

}
