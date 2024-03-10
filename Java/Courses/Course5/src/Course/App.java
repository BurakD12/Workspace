package Course;
public class App {
    public static void main(String[] args) throws Exception {
        // ClassName objName = new Constructor()
        // Def
        Person burak = new Person();

        // Params
        Person ayberk = new Person("Ayberk",24,194);

        // Copy
        Person mehmet = new Person(ayberk);

        burak.introduceMyself();
        ayberk.introduceMyself();
        mehmet.introduceMyself();
    }
}
