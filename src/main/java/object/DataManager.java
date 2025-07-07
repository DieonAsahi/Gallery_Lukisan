package object;

import java.util.ArrayList;
import java.util.List;

public class DataManager<T> {
    private List<T> listData = new ArrayList<>();

    public void tambah(T data) {
        listData.add(data);
    }

    public List<T> ambilSemua() {
        return listData;
    }

    public void hapus(T data) {
        listData.remove(data);
    }

    public int jumlah() {
        return listData.size();
    }
}
