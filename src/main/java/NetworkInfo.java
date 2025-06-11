import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;

public class NetworkInfo {
    public static void main(String[] args) {
        SystemInfo si = new SystemInfo();
        List<NetworkIF> networkIFs = si.getHardware().getNetworkIFs();

        for (NetworkIF net : networkIFs) {
            net.updateAttributes(); // Cập nhật thông tin mới nhất

            System.out.println("Tên giao diện: " + net.getName());
            System.out.println("Địa chỉ MAC: " + net.getMacaddr());
            System.out.println("Tốc độ: " + net.getSpeed() / 1e6 + " Mbps");
            System.out.println("Đã nhận: " + formatBytes(net.getBytesRecv()));
            System.out.println("Đã gửi: " + formatBytes(net.getBytesSent()));
            System.out.println("------------------------");
        }
    }

    private static String formatBytes(long bytes) {
        return String.format("%.2f MB", bytes / 1e6);
    }
}
