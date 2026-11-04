package vn.sonbds.marketplace.realestate;

public final class MapCoordinates {
    private MapCoordinates() {
    }

    public static boolean areComplete(Double latitude, Double longitude) {
        return (latitude == null) == (longitude == null);
    }

    public static void validate(Double latitude, Double longitude) {
        if (!areComplete(latitude, longitude)) {
            throw new IllegalArgumentException("Vĩ độ và kinh độ phải được nhập cùng nhau.");
        }
        if (latitude != null && (!Double.isFinite(latitude) || latitude < -90 || latitude > 90)) {
            throw new IllegalArgumentException("Vĩ độ phải nằm trong khoảng -90 đến 90.");
        }
        if (longitude != null && (!Double.isFinite(longitude) || longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("Kinh độ phải nằm trong khoảng -180 đến 180.");
        }
    }
}
