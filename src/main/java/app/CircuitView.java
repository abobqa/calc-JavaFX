package app;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

public class CircuitView extends Canvas {

    public CircuitView() {
        setWidth(680);
        setHeight(360);
    }

    public void render(List<Double> rTop, List<Double> rBot, double vin, double vout) {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());

        double W = getWidth();
        double H = getHeight();

        // немного сдвигаем всё ближе к центру
        double xLeft = 130;
        double xRight = W - 180; // было -90
        double yTop = 120;
        double yBot = 220;
        double yMid = (yTop + yBot) / 2;

        g.setStroke(Color.BLACK);
        g.setLineWidth(2);
        g.setFill(Color.BLACK);
        g.setFont(Font.font(13));

        // Левая вертикальная шина
        g.strokeLine(xLeft, yTop - 40, xLeft, yBot + 40);
        g.fillOval(xLeft - 3, yMid - 3, 6, 6);
        g.fillText("Vout = " + fmt(vout) + "V", xLeft - 80, yMid + 4);

        // Верхняя ветвь (резисторы -> Vin)
        drawSeriesHorizontal(g, xLeft, xRight, yTop, rTop, "R1");
        // Соединение Vin
        g.strokeLine(xRight, yTop, xRight + 30, yTop);
        g.fillOval(xRight + 30 - 3, yTop - 3, 6, 6);
        g.fillText("Vin = " + fmt(vin) + "V", xRight + 40, yTop - 8);

        // Нижняя ветвь (резисторы -> GND)
        drawSeriesHorizontal(g, xLeft, xRight, yBot, rBot, "R2");
        g.strokeLine(xRight, yBot, xRight + 30, yBot);
        g.strokeLine(xRight + 30, yBot, xRight + 30, yBot + 20);
        drawGround(g, xRight + 30, yBot + 20);
        g.fillText("GND", xRight + 15, yBot + 44);
    }

    /** Рисуем серию резисторов с аккуратными подписями и компактной длиной */
    private void drawSeriesHorizontal(GraphicsContext g, double x1, double x2, double y,
                                      List<Double> parts, String baseLabel) {
        int n = Math.max(1, parts.size());
        double lead = 16;   // короткие провода
        double gap = 50;    // промежуток между резисторами
        double rw = 100;     // ширина резистора (ещё короче)
        double rh = 12;

        // Провод от шины
        g.strokeLine(x1, y, x1 + lead, y);
        double x = x1 + lead;

        for (int i = 0; i < n; i++) {
            double rValue = parts.get(Math.min(i, parts.size() - 1));
            g.strokeRect(x, y - rh / 2, rw, rh);
            g.fillText(baseLabel + (n > 1 ? (i + 1) : "") + "=" + fmtOhm(rValue),
                    x + 2, y - rh / 2 - 5);

            x += rw;
            if (i < n - 1) {
                g.strokeLine(x, y, x + gap, y);
                x += gap;
            }
        }

        g.strokeLine(x, y, x2, y);
    }

    private void drawGround(GraphicsContext g, double x, double y) {
        double w = 18, step = 4;
        g.strokeLine(x - w / 2, y, x + w / 2, y);
        g.strokeLine(x - w / 3, y + step, x + w / 3, y + step);
        g.strokeLine(x - w / 6, y + 2 * step, x + w / 6, y + 2 * step);
    }

    private String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "-";
        if (v >= 1_000_000) return String.format("%.2fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.2fk", v / 1_000.0);
        return String.format("%.2f", v);
    }

    private String fmtOhm(double r) {
        if (r >= 1_000_000) return String.format("%.2fM\u03A9", r / 1_000_000.0);
        if (r >= 1_000) return String.format("%.2fk\u03A9", r / 1_000.0);
        return String.format("%.2f\u03A9", r);
    }
}
