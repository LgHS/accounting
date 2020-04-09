package be.lghs.accounting.services;

import be.lghs.accounting.model.tables.records.SubscriptionsRecord;
import be.lghs.accounting.repositories.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.Month;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jooq.Result;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.TimeZone;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    public static final int NUMBER_OF_MONTHS_TO_LOAD = 12;

    private final SubscriptionRepository subscriptionRepository;

    public void generateMonthlyGraphForUser(UUID userId, OutputStream output, int width, int height, boolean drawTitle) throws IOException {
        Result<SubscriptionsRecord> subscriptions = subscriptionRepository.findSubscriptionsForMonthlyGraph(userId, NUMBER_OF_MONTHS_TO_LOAD);

        generateGraphForUser(subscriptions,
                "Months with a valid subscription paid",
                new Color(0x5555ff),
                output,
                width,
                height,
                drawTitle);
    }

    public void generateYearlyGraphForUser(UUID userId, OutputStream output, int width, int height, boolean drawTitle) throws IOException {
        Result<SubscriptionsRecord> subscriptions = subscriptionRepository.findSubscriptionsForYearlyGraph(userId);

        generateGraphForUser(subscriptions,
                "Years with a valid subscription paid",
                new Color(0x9D7146),
                output,
                width,
                height,
                drawTitle);
    }

    public void generateGraphForUser(Result<SubscriptionsRecord> subscriptions,
                                     String title,
                                     Color color,
                                     OutputStream output,
                                     int width,
                                     int height,
                                     boolean drawTitle) throws IOException {
        var payments = new TimeSeries("payments");

        for (SubscriptionsRecord record : subscriptions) {
            var startDate = record.getStartDate();
            var endDate = record.getEndDate();
            var currentMonth = startDate;

            while (currentMonth.isBefore(endDate)) {
                var month = new Month(currentMonth.getMonthValue(), currentMonth.getYear());
                payments.addOrUpdate(month, 1);

                currentMonth = currentMonth.plusMonths(1);
            }
        }

        var dataset = new TimeSeriesCollection(TimeZone.getTimeZone("Europe/Brussels"));
        dataset.addSeries(payments);

        LocalDate minMonth = LocalDate.now().withDayOfMonth(1).minusMonths(NUMBER_OF_MONTHS_TO_LOAD);
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        Month firstMonthToShow = new Month(minMonth.getMonthValue(), minMonth.getYear());
        Month lastMonthToShow = new Month(thisMonth.getMonthValue(), thisMonth.getYear());

        var hidden = new TimeSeries("only used to make sure the graph is at least "+ NUMBER_OF_MONTHS_TO_LOAD + " months long and showing the current month");
        hidden.add(firstMonthToShow, 0);
        hidden.add(lastMonthToShow, 0);
        dataset.addSeries(hidden);

        var chart = ChartFactory.createXYBarChart(
            title,
            null,
            true,
            null,
            dataset,
            PlotOrientation.VERTICAL,
            false, false, false);

        if (!drawTitle) {
            chart.setTitle((String) null);
        }

        chart.getXYPlot().addDomainMarker(new ValueMarker(
                Instant.now().toEpochMilli(),
                new Color(0x80FF7F),
                new BasicStroke(4)));


        chart.getXYPlot().getRenderer().setSeriesPaint(0, color);

        var dateFormat = new SimpleDateFormat("MMM YY");
        ((DateAxis) chart.getXYPlot().getDomainAxis()).setTickUnit(new DateTickUnit(
            DateTickUnitType.MONTH, 1,
            dateFormat));
        chart.getXYPlot().getRangeAxis().setVisible(false);

        GraphService.writeChartToOutputStream(output, chart, width, height);
    }
}
