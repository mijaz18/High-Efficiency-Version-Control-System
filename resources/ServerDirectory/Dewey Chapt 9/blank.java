import java.awt.Point; //must import before class
import java.awt.Rectangle; //must import before class
public class blank {
    public static void main(String[] args) {
        Point blank;
        blank = new Point(3,4); //instance variables make up an object
                                //each object has its own copy of instance variables
        int x=blank.x; //blank specifies which object to get instance variable  from
        System.out.println(x);
    }
}