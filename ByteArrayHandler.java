import java.util.concurrent.atomic.AtomicBoolean;

public class ByteArrayHandler {
    static byte[] mergeConstructor(byte[] one, byte[] two) {
        return mergeFunc(one, two);
    }

    static byte[] mergewithByteConstructor(byte[] one, byte two) {
        return mergewithByteFunc(one,two);
    }

    static byte[] intToByteArray(int id) {
        return  intToByteArrayConstructor(id);
    }

    private static byte[] mergeFunc(byte[] one,byte[] two){
        byte[] response;
        response = new byte[one.length + two.length];
        System.arraycopy(one, 0, response, 0, one.length);
        System.arraycopy(two, 0, response, one.length, two.length);
        return response;
    }

    private static byte[] mergewithByteFunc(byte[] one,byte two){
        byte[] response = new byte[one.length + 1];
        System.arraycopy(one, 0, response, 0, one.length);
        response[one.length] = two;
        return response;
    }

    static int conversionToInt(byte[] num) {
        int res0 = ((num[0] & 0xFF) << 24);
        int res1 = ((num[1] & 0xFF) << 16);
        int res2 = ((num[2] & 0xFF) << 8);
        int res3 = (num[3] & 0xFF);
        return res0 | res1 | res2 | res3;
    }

    private static byte[] intToByteArrayConstructor(int id){
        byte[] res;
        res = new byte[4];
        res[0] = (byte) ((id >> 24) & 0xFF);
        res[1] = (byte) ((id >> 16) & 0xFF);
        res[2] = (byte) ((id >> 8) & 0xFF);
        res[3] = (byte) (id & 0xFF);
        return res;
    }


    static byte[] boolArraytoByteArray(AtomicBoolean[] boolArray, byte[] byteArray) {
        for (int idx = 0; idx < boolArray.length; idx++) {
            if (boolArray[idx].get()) {
                byteArray[idx / 8] |= 1 << (7 - (idx % 8));
            }
        }
        return byteArray;
    }
}