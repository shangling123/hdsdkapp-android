package jd.hd.seller.hd_shop_sdk.app;

public class App {

    public static void main(String[] args) {
        String aaa = "aaa";
        final String bbb = aaa;
        aaa = "ccc";
        System.out.printf(aaa);
        System.out.printf(bbb);
    }
}
