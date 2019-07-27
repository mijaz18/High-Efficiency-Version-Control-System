import java.awt.Point;
public class Distance {
    public static void main(String[] args) {
        Point a=new Point(1,2);
        Point b=new Point(3,4);
        double c=DistanceBetweenPoints.distance(a, b);
        System.out.println(c);
    }
}