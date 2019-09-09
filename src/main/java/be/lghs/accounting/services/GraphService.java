package be.lghs.accounting.services;

import be.lghs.accounting.repositories.GraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.PlotOrientation;
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
import java.sql.Date;
import java.util.TimeZone;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    static {
        // Remove stupid gradients
        ChartFactory.setChartTheme(StandardChartTheme.createLegacyTheme());

        // Remove stupid shadows
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter() {
            @Override
            public void paintBarShadow(Graphics2D g2, XYBarRenderer renderer, int row,
                                       int column, RectangularShape bar, RectangleEdge base,
                                       boolean pegShadow) {}
        });
    }

    private final GraphRepository graphRepository;

    public void generateRollingSumGraph(OutputStream output) throws IOException {
        var series = new TimeSeries("name");
        var values = graphRepository.rollingSum();

        for (Record2<Date, BigDecimal> record : values) {
            var localDate = record.component1().toLocalDate();
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

        int width = 1080;
        int height = 480;

        var g2 = new SVGGraphics2D(width, height);
        var r = new Rectangle(0, 0, width, height);
        chart.draw(g2, r);

        var writer = new OutputStreamWriter(output);
        writer.write(g2.getSVGElement());
        writer.flush();
    }

    public void generateCreditsPerDayGraph(OutputStream output) throws IOException {
        var credits = new TimeSeries("credits");
        var debits = new TimeSeries("debits");
        var values = graphRepository.creditsPerDay();

        for (Record3<Date, BigDecimal, BigDecimal> record : values) {
            var localDate = record.component1().toLocalDate();
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

        chart.setBackgroundPaint(null);
        chart.setBorderPaint(Color.BLACK);
        chart.setBorderVisible(false);
        chart.getXYPlot().getRenderer().setSeriesPaint(0, Color.BLUE);
        chart.getXYPlot().getRenderer().setSeriesPaint(1, Color.RED);

        int width = 1080;
        int height = 480;

        var g2 = new SVGGraphics2D(width, height);
        var r = new Rectangle(0, 0, width, height);
        chart.draw(g2, r);

        var writer = new OutputStreamWriter(output);
        writer.write(g2.getSVGElement());
        writer.flush();
    }
}
