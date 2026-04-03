package dripnote.common.response;

public record ApiResponse<T>(
        String statusCode,
        String message,
        T data
) {
    public static <T> ApiResponse ok(T data) {
        return new ApiResponse<>("200", "OK", data);
    }
}
