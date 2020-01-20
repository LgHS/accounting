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
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    public static final int NUMBER_OF_MONTHS_TO_LOAD = 12;

    private final SubscriptionRepository subscriptionRepository;

    public void generateMonthlyGraphForUser(UUID userId, OutputStream output, int width) throws IOException {
        var subscriptions = subscriptionRepository.findSubscriptionsForMonthlyGraph(userId, NUMBER_OF_MONTHS_TO_LOAD);
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

        LocalDate now = LocalDate.now().withDayOfMonth(1).minusMonths(NUMBER_OF_MONTHS_TO_LOAD);
        Month firstMonthToShow = new Month(now.getMonthValue(), now.getYear());

        var hidden = new TimeSeries("only used to set the minY to 0 on the graph");
        hidden.add(firstMonthToShow, 0);
        dataset.addSeries(hidden);

        // var chart = ChartFactory.createTimeSeriesChart(
        //     "Money in HS accounts over time",
        //     null,
        //     "EUR",
        //     dataset, false, false, false);
        var chart = ChartFactory.createXYBarChart(
            "Months with a valid subscription paid",
            null,
            true,
            null,
            dataset,
            PlotOrientation.VERTICAL,
            false, false, false);

        chart.getXYPlot().addDomainMarker(new ValueMarker(Instant.now().toEpochMilli(), new Color(0x00FF00), new BasicStroke(2)));


        chart.getXYPlot().getRenderer().setSeriesPaint(0, new Color(0x5555ff));

        var dateFormat = new SimpleDateFormat("MMM YY");
        ((DateAxis) chart.getXYPlot().getDomainAxis()).setTickUnit(new DateTickUnit(
            DateTickUnitType.MONTH, 1,
            dateFormat));
        chart.getXYPlot().getRangeAxis().setVisible(false);

        GraphService.writeChartToOutputStream(output, chart, width, 100);
    }
}
