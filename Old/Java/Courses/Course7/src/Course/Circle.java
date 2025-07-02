package Course;


public class Circle extends Shape implements Resizable{
    private double radius;

    Circle(String color, boolean filled, double radius){
        super(color, filled);
        this.radius = radius;
    }

    @Override
    double getArea() {
        return Math.PI * Math.pow(radius,2);
    }

    @Override
    double getPerimeter() {
        return Math.PI * radius * 2;
    }

    @Override
    public String toString() {
        return "Circle[color=" + this.getColor() + ", filled=" + this.isFilled() + ", radius=" + radius + "]";
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public void resize(double factor) {
        this.radius *= factor;
    }
}
