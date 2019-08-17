package be.lghs.accounting.services;

import be.lghs.accounting.repositories.GraphRepository;
import lombok.RequiredArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jooq.Record2;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
public class GraphService {

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

        var chart = ChartFactory.createTimeSeriesChart(
            "Money in HS accounts over time",null,
            "EUR",
            dataset,false,false,false);

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
}
