package com.github.zawataki;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A library for getting an intersection point of lines or rectangles
 *
 * @see <a href="https://stackoverflow.com/a/15594751/9246253">java - Line
 * crosses Rectangle - how to find the cross points? - Stack Overflow</a>
 */
public class IntersectionPoint {

    /**
     * Get intersection point from given two lines. If intersection point is on
     * endpoint of a line, it's regarded as not intersected.
     *
     * @param l1 1st line
     * @param l2 2nd line
     *
     * @return A intersection point if exists. Otherwise empty.
     */
    static public Optional<Point2D> getIntersectionPoint(Line2D l1, Line2D l2) {
        return getIntersectionPoint(l1, l2, false);
    }

    /**
     * Get intersection point from given two lines
     *
     * @param l1 1st line
     * @param l2 2nd line
     * @param includesEndpoint Includes endpoint of line if {@code true}.
     * Otherwise excludes.
     *
     * @return A intersection point if exists. Otherwise empty.
     */
    static public Optional<Point2D> getIntersectionPoint(Line2D l1, Line2D l2,
            boolean includesEndpoint) {

        // Line AB represented as a1x + b1y = c1
        final double a1 = l1.getP2().getY() - l1.getP1().getY();
        final double b1 = l1.getP1().getX() - l1.getP2().getX();

        // Line CD represented as a2x + b2y = c2
        final double a2 = l2.getP2().getY() - l2.getP1().getY();
        final double b2 = l2.getP1().getX() - l2.getP2().getX();

        final double determinant = a1 * b2 - a2 * b1;
        if (determinant == 0) {
            return Optional.empty();
        }

        final double c1 = a1 * (l1.getP1().getX()) + b1 * (l1.getP1().getY());
        final double c2 = a2 * (l2.getP1().getX()) + b2 * (l2.getP1().getY());

        final double x = (b2 * c1 - b1 * c2) / determinant + 0.0;
        final double y = (a1 * c2 - a2 * c1) / determinant + 0.0;

        final Point2D crossPoint =
                new Point2D.Double(doubleToBigDecimal(x).doubleValue(),
                        doubleToBigDecimal(y).doubleValue());

        if (!pointIsOnLine(crossPoint, l1, includesEndpoint) ||
                !pointIsOnLine(crossPoint, l2, includesEndpoint)) {
            return Optional.empty();
        }

        return Optional.of(crossPoint);
    }

    /**
     * A given point is on a given line or not
     *
     * @param point the specified point
     * @param line the specified line
     * @param includesEndpoint Includes endpoint of line if {@code true}.
     * Otherwise excludes.
     *
     * @return {@code true} if a point is on a line. Otherwise {@code false}
     */
    static public boolean pointIsOnLine(Point2D point, Line2D line,
            boolean includesEndpoint) {

        if (!includesEndpoint && pointIsOnLineEndpoint(point, line)) {
            return false;
        }

        final BigDecimal distanceBetweenPointAndLineP1 =
                doubleToBigDecimal(point.distance(line.getP1()));
        final BigDecimal distanceBetweenPointAndLineP2 =
                doubleToBigDecimal(point.distance(line.getP2()));
        final BigDecimal distanceBetweenPointAndLineEndpoint =
                distanceBetweenPointAndLineP1.add(distanceBetweenPointAndLineP2)
                        .stripTrailingZeros();
        final BigDecimal lineLength =
                doubleToBigDecimal(line.getP1().distance(line.getP2()));

        return distanceBetweenPointAndLineEndpoint.equals(lineLength) ||
                (distanceBetweenPointAndLineEndpoint.subtract(lineLength)
                        .abs()
                        .doubleValue() == 0.00001);
    }

    static private BigDecimal doubleToBigDecimal(double value) {
        return BigDecimal.valueOf(value)
                .setScale(5, RoundingMode.DOWN)
                .stripTrailingZeros();
    }

    /**
     * Get intersection points from a given rectangle and a line.
     *
     * @param rect the specified rectangle
     * @param line the specified line
     * @param includesEndpoint If {@code true}, it's regarded as intersected in
     * case of the followings.
     * <ul>
     * <li>an intersection point is on endpoint of a given line</li>
     * <li>an intersection point is on any of vertex of given rectangle</li>
     * </ul>
     * If {@code false} and same situation, it's regarded as NOT intersected.
     * But if {@code false} and the number of intersection points is greater
     * than 1, it's regarded as intersected.
     *
     * @return list of intersection points if exists. Otherwise empty list.
     */
    static public List<Point2D> getIntersectionPoints(Rectangle2D rect,
            Line2D line, boolean includesEndpoint) {

        final List<Line2D> linesOfRectangle = getLinesOfRectangle(rect);

        final List<Point2D> intersectionPoints = linesOfRectangle.stream()
                .map(lineOfRect -> getIntersectionPoint(lineOfRect, line,
                        true).orElse(null))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (includesEndpoint || intersectionPoints.size() != 1) {
            return intersectionPoints;
        }

        final Point2D point = intersectionPoints.get(0);

        if (pointIsOnLineEndpoint(point, line) || linesOfRectangle.stream()
                .anyMatch(li -> pointIsOnLineEndpoint(point, li))) {

            return new ArrayList<>();
        }

        return intersectionPoints;
    }

    static private List<Line2D> getLinesOfRectangle(Rectangle2D rect) {

        final Point2D upperLeftPoint =
                new Point2D.Double(rect.getX(), rect.getY());
        final Point2D upperRightPoint =
                new Point2D.Double(rect.getX() + rect.getWidth(), rect.getY());
        final Point2D lowerLeftPoint =
                new Point2D.Double(rect.getX(), rect.getY() - rect.getHeight());
        final Point2D lowerRightPoint =
                new Point2D.Double(rect.getX() + rect.getWidth(),
                        rect.getY() - rect.getHeight());

        List<Line2D> linesOfRect = new ArrayList<>();
        // upper line
        linesOfRect.add(new Line2D.Double(upperLeftPoint, upperRightPoint));
        // lower line
        linesOfRect.add(new Line2D.Double(lowerLeftPoint, lowerRightPoint));
        // left line
        linesOfRect.add(new Line2D.Double(upperLeftPoint, lowerLeftPoint));
        // right line
        linesOfRect.add(new Line2D.Double(upperRightPoint, lowerRightPoint));

        return linesOfRect;
    }

    static private boolean pointIsOnLineEndpoint(Point2D point, Line2D line) {
        return point.equals(line.getP1()) || point.equals(line.getP2());
    }
}
