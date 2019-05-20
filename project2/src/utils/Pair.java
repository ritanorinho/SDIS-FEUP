package utils;

import java.io.Serializable;

public class Pair<T, U> implements Serializable{

    private static final long serialVersionUID = 1L;
    private T left;
    private U right;
    
    public Pair(T left, U right){
        this.left = left;
        this.right = right;
    }

    public T getKey(){
        return left;
    }

    public U getValue(){
        return right;
    }

}