import java.awt.Point;
public class DistanceBetweenPoints {
    public static double distance(Point p1, Point p2) {
            double dx=p1.x-p2.x;
            double dy=p1.y-p2.y;
            return Math.sqrt(dx*dx+dy*dy);
    }
                           
}
    