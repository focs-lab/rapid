package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class PipedDeepCopy {
    public static Object copy(Object object) {
        Object obj = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            oos.flush();
            oos.close();
            bos.close();
            byte[] byteData = bos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
            ObjectInputStream ios = new ObjectInputStream(bais);
            obj = ios.readObject();
            bais.close();
            ios.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

}
