package Course;
public class ShapeDemo {
    public static void main(String[] args) {
        Circle circle = new Circle("red", true, 5.0);
        Rectangle rectangle = new Rectangle("blue", false, 4.0, 6.0);

        System.out.println("Original " + circle);
        System.out.println("Original area of circle: " + circle.getArea());
        circle.resize(2);
        System.out.println("Resized " + circle);
        System.out.println("Resized area of circle: " + circle.getArea());

        System.out.println();

        System.out.println("Original " + rectangle);
        System.out.println("Original area of rectangle: " + rectangle.getArea());
        rectangle.resize(1.5);
        System.out.println("Resized " + rectangle);
        System.out.println("Resized area of rectangle: " + rectangle.getArea());
    }
}