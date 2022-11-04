public class App {
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            System.gc();

            byte[] b = new byte[1024 * 1024];
            Thread.sleep(1000);
        }
    }
}
