public class Encryption {


    public static String XOR(String message , String key){
        int j = 0;
        StringBuilder  finalMessage = new StringBuilder();
        for(int i = 0; i < message.length(); i++) {
            finalMessage.append((char) (message.charAt(i) ^ key.charAt(j)));
            j++;
            if(j == key.length()) {
                j=0;
            }
        }
        return finalMessage.toString();
    }

}
