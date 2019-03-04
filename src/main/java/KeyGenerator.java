import java.util.Random;

public class KeyGenerator {

    private Random random = new Random();

    public String generate() {

        String DIGITS = "0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10 ;i++){
            int position = random.nextInt(DIGITS.length());
            sb.append(DIGITS.charAt(position));
        }

        return sb.toString();
    }
}
