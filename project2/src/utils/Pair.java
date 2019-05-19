package utils;

public class Pair<T, U> {

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