package it.damianogiusti.rxrealmrecyclerviewadapter;

import java.lang.reflect.Field;

/**
 * Created by Damiano Giusti on 22/01/17.
 */
final class Utils {
    static <T> boolean areObjectsEquals(Class<? extends T> type, T obj1, T obj2) {
        for (Field field : type.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                // if the two fields are null, continue to next iteration
                if (field.get(obj1) == null && field.get(obj2) == null)
                    continue;
                // if at least one is null, return false
                if (field.get(obj1) == null ^ field.get(obj2) == null)
                    return false;
                // fields aren't null, if they are different, return false
                if (!field.get(obj1).equals(field.get(obj2)))
                    return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
