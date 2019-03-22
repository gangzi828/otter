package com.alibaba.otter.node.etl;

/**
 * @author by gangzi on 2019/3/22 2:47 PM.
 */
public class CheckSum {
    public static int makeChecksum(String data) {
        if (data == null || data.equals("")) {
            return 0;
        }
        int total = 0x0;
        int len = data.length();
        int num = 0;

        while (num < len) {

            String s = data.substring(num, num + 4>len?len:num+4);

            total += Integer.parseInt(s, 16);
            num = num + 4;
        }

        while (total > 0xFFFF) {
            int low = total & 0xFFFF;
            int high = total >> 16;
            total = low + high;
        }
        return 0xFFFF-total;
    }

    public static void main(String[] args) {

        System.out.println(makeChecksum("0100003d0069126100000000011730020002011820033030300119201130312e30332e30302053564e3a32313439012b200b4a55423636303232313937"));
    }
}

