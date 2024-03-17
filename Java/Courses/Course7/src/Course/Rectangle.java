package Course;
public class Rectangle extends Shape implements Resizable{
    private double width;
    private double length;

    Rectangle(String color, boolean filled, double width, double length){
        super(color, filled);
        this.width = width;
        this.length = length;
    }

    @Override
    double getArea() {
        return width * length;
    }

    @Override
    double getPerimeter() {
        return (2 * width) + (2 * length);
    }

    @Override
    public String toString() {
        return "Shape[color=" + this.getColor() + ", filled=" + this.isFilled() + ", width=" + width + ", length=" + length + "]";
    }

    @Override
    public void resize(double factor) {
        this.width *= factor;
        this.length *= factor;
    }
}
