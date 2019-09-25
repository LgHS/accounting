package be.lghs.accounting.services;

import be.lghs.accounting.repositories.GraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jooq.Record2;
import org.jooq.Record3;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.geom.RectangularShape;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    static {
        // Remove stupid gradients
        ChartFactory.setChartTheme(new StandardChartTheme("LGHS", false) {
            @Override
            public void apply(JFreeChart chart) {
                chart.setBackgroundPaint(null);
                chart.setBorderVisible(false);
                var plot = chart.getPlot();
                if (plot instanceof XYPlot) {
                    var xyPlot = (XYPlot) plot;
                    xyPlot.setOutlineVisible(false);
                    xyPlot.getDomainAxis().setAxisLineVisible(false);
                    xyPlot.getRangeAxis().setAxisLineVisible(false);
                    xyPlot.getRangeAxis().setTickMarksVisible(false);
                }
            }
        });

        // Remove stupid shadows
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter() {
            @Override
            public void paintBarShadow(Graphics2D g2, XYBarRenderer renderer, int row,
                                       int column, RectangularShape bar, RectangleEdge base,
                                       boolean pegShadow) {}
        });
    }
    private static final int WIDTH = 500;
    private static final int HEIGHT = (int) (WIDTH / 16.0 * 9);

    private final GraphRepository graphRepository;

    public void generateRollingSumGraph(OutputStream output) throws IOException {
        var series = new TimeSeries("name");
        var values = graphRepository.rollingSum();

        for (Record2<LocalDate, BigDecimal> record : values) {
            var localDate = record.component1();
            var day = new Day(localDate.getDayOfMonth(), localDate.getMonthValue(), localDate.getYear());
            series.add(day, record.component2());
        }

        var dataset = new TimeSeriesCollection(TimeZone.getTimeZone("Europe/Brussels"));
        dataset.addSeries(series);

        var hidden = new TimeSeries("only used to set the minY to 0 on the graph");
        hidden.add(series.getDataItem(0).getPeriod(), 0);
        dataset.addSeries(hidden);

        var chart = ChartFactory.createTimeSeriesChart(
            "Money in HS accounts over time", null,
            "EUR",
            dataset, false, false, false);

        chart.setBackgroundPaint(null);
        chart.setBorderPaint(Color.BLACK);
        chart.setBorderVisible(false);
        var dateFormat = new SimpleDateFormat("MMM YY");
        var unit = new DateTickUnit(
            DateTickUnitType.MONTH, 6,
            dateFormat);
        try {
            var minorTickCount = TickUnit.class.getDeclaredField("minorTickCount");
            minorTickCount.setAccessible(true);
            minorTickCount.set(unit, 12);
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
        ((DateAxis) chart.getXYPlot().getDomainAxis()).setTickUnit(unit);

        writeChartToOutputStream(output, chart);
    }

    public void generateCreditsPerDayGraph(OutputStream output) throws IOException {
        var credits = new TimeSeries("credits");
        var debits = new TimeSeries("debits");
        var values = graphRepository.creditsPerDay();

        for (Record3<LocalDate, BigDecimal, BigDecimal> record : values) {
            var localDate = record.component1();
            var day = new Day(localDate.getDayOfMonth(), localDate.getMonthValue(), localDate.getYear());
            credits.add(day, record.component2());
            debits.add(day, record.component3());
        }

        var dataset = new TimeSeriesCollection(TimeZone.getTimeZone("Europe/Brussels"));
        dataset.addSeries(credits);
        dataset.addSeries(debits);

        var hidden = new TimeSeries("only used to set the minY to 0 on the graph");
        hidden.add(credits.getDataItem(0).getPeriod(), 0);
        dataset.addSeries(hidden);

        // var chart = ChartFactory.createTimeSeriesChart(
        //     "Money in HS accounts over time",
        //     null,
        //     "EUR",
        //     dataset, false, false, false);
        var chart = ChartFactory.createXYBarChart(
            "Money entering HS accounts per day",
            null,
            true,
            "EUR",
            dataset,
            PlotOrientation.VERTICAL,
            false, false, false);


        chart.getXYPlot().getRenderer().setSeriesPaint(0, new Color(0x5555ff));
        chart.getXYPlot().getRenderer().setSeriesPaint(1, new Color(0xff5555));

        var dateFormat = new SimpleDateFormat("MMM YY");
        ((DateAxis) chart.getXYPlot().getDomainAxis()).setTickUnit(new DateTickUnit(
            DateTickUnitType.MONTH, 1,
            dateFormat));

        writeChartToOutputStream(output, chart);
    }

    private void writeChartToOutputStream(OutputStream output, JFreeChart chart) throws IOException {
        var g2 = new SVGGraphics2D(WIDTH, HEIGHT);
        var r = new Rectangle(0, 0, WIDTH, HEIGHT);
        chart.draw(g2, r);

        var writer = new OutputStreamWriter(output);
        writer.write(g2.getSVGElement());
        writer.flush();
    }

    public void generateSubscriptionGraph(UUID userId, OutputStream output) {
    }
}
