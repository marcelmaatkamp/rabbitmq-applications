package org.application.rabbitmq.stream.util;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by marcelmaatkamp on 25/11/15.
 */
public class SortedArrayList<T> extends ArrayList<T> {

    @SuppressWarnings("unchecked")
    public void insertSorted(T value) {
        add(value);
        Comparable<T> cmp = (Comparable<T>) value;
        for (int i = size() - 1; i > 0 && cmp.compareTo(get(i - 1)) < 0; i--)
            Collections.swap(this, i, i - 1);
    }
}
