package utils;

import java.net.InetAddress;

public class Pair {

    private int left;
    private InetAddress right;
    
    public Pair(int left,InetAddress right){
        this.left = left;
        this.right = right;
    }

    public int getKey(){
        return left;
    }

    public InetAddress getValue(){
        return right;
    }

}