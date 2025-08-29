package com.andd.DoDangAn.DoDangAn.services.Cache;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Giao diện cho các thao tác với Redis, hỗ trợ cache và thông báo real-time trong ứng dụng e-commerce.
 */
public interface BaseRedisService {

    /**
     * Lưu giá trị đơn giản vào Redis với key.
     * @param key Khóa Redis.
     * @param value Giá trị cần lưu.
     */
    void set(String key, Object value);

    /**
     * Đặt thời gian sống (TTL) cho một key trong Redis.
     * @param key Khóa Redis.
     * @param timeoutInDays Thời gian sống (tính bằng ngày).
     */
    void setTimeToLive(String key, int timeoutInDays);

    /**
     * Lưu một field-value vào hash trong Redis.
     * @param key Khóa của hash.
     * @param field Tên field trong hash.
     * @param value Giá trị của field.
     */
    void hashSet(String key, String field, Object value);

    /**
     * Kiểm tra xem một field có tồn tại trong hash không.
     * @param key Khóa của hash.
     * @param field Tên field cần kiểm tra.
     * @return true nếu field tồn tại, false nếu không.
     */
    boolean hashExists(String key, String field);

    /**
     * Lấy giá trị từ Redis theo key.
     * @param key Khóa Redis.
     * @return Giá trị tương ứng hoặc null nếu không tồn tại.
     */
    Object get(String key);

    Map<String, Object> getMap(String key);

    /**
     * Lấy tất cả field-value của một hash trong Redis.
     * @param key Khóa của hash.
     * @return Map chứa các cặp field-value.
     */
    Map<String, Object> getField(String key);

    /**
     * Lấy giá trị của một field cụ thể trong hash.
     * @param key Khóa của hash.
     * @param field Tên field.
     * @return Giá trị của field hoặc null nếu không tồn tại.
     */
    Object hashGet(String key, String field);

    /**
     * Lấy danh sách các giá trị trong hash có field bắt đầu bằng prefix.
     * @param key Khóa của hash.
     * @param fieldPrefix Prefix của field.
     * @return Danh sách các giá trị tương ứng.
     */
    List<Object> hashGetByFieldPrefix(String key, String fieldPrefix);

    /**
     * Lấy tập hợp các field trong hash.
     * @param key Khóa của hash.
     * @return Tập hợp các field.
     */
    Set<String> getFieldPrefix(String key);

    /**
     * Xóa một key khỏi Redis.
     * @param key Khóa cần xóa.
     */
    void delete(String key);

    /**
     * Xóa một field cụ thể trong hash.
     * @param key Khóa của hash.
     * @param fieldPrefix Field cần xóa.
     */
    void deleteByFieldPrefix(String key, String fieldPrefix);

    /**
     * Xóa nhiều field trong hash.
     * @param key Khóa của hash.
     * @param fields Danh sách các field cần xóa.
     */
    void deleteByKey(String key, List<String> fields);

    /**
     * Gửi thông báo real-time qua Redis pub/sub.
     * @param channel Kênh Redis để publish.
     * @param message Thông điệp cần gửi.
     */
    void publish(String channel, Object message);
}