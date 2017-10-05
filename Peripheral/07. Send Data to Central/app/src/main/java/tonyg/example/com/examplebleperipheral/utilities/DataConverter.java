package tonyg.example.com.examplebleperipheral.utilities;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Convert data formats
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2015-12-21
 */

public class DataConverter {

    /**
     * convert bytes to hexadecimal for debugging purposes
     *
     * @param intValue the data to convert
     * @param length the max length of the data
     * @return byte array version of the intValue
     */
    public static byte[] intToBytes(int intValue, int length) {
        ByteBuffer b = ByteBuffer.allocate(length);

        // BLE Data is always little-endian
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(intValue);

        return b.array();
    }

    /**
     * Generate a random String
     *
     * @param int length of resulting String
     * @return random String
     */
    private static final String ALLOWED_CHARACTERS ="0123456789abcdefghijklmnopqrstuvwxyz";
    public static String getRandomString(final int length) {
        final Random random = new Random();
        final StringBuilder sb=new StringBuilder(length);
        for(int i=0;i<length;++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}
