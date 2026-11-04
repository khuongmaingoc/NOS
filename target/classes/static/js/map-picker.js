(() => {
    const tileUrl = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';
    const attribution = '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap contributors</a>';
    const vietnamCenter = [16.0, 108.0];

    function showStatus(element, message, isError = false) {
        if (!element) return;
        element.textContent = message;
        element.classList.toggle('is-error', isError);
    }

    function createMap(canvas, latitude, longitude, interactive) {
        const hasCoordinates = Number.isFinite(latitude) && Number.isFinite(longitude);
        const map = L.map(canvas, { scrollWheelZoom: false }).setView(
            hasCoordinates ? [latitude, longitude] : vietnamCenter,
            hasCoordinates ? 15 : 5
        );
        const tileLayer = L.tileLayer(tileUrl, {
            maxZoom: 19,
            attribution,
            crossOrigin: true
        }).addTo(map);
        let marker = null;

        function setMarker(lat, lng, draggable = false) {
            if (marker) marker.remove();
            marker = L.marker([lat, lng], { draggable }).addTo(map);
            return marker;
        }

        if (hasCoordinates) setMarker(latitude, longitude, interactive);
        window.setTimeout(() => map.invalidateSize(), 0);
        return { map, tileLayer, setMarker, getMarker: () => marker };
    }

    function initializePicker(element) {
        const latitudeInput = document.querySelector(element.dataset.latitudeSelector);
        const longitudeInput = document.querySelector(element.dataset.longitudeSelector);
        const canvas = element.querySelector('[data-map-canvas]');
        const status = element.querySelector('[data-map-status]');
        if (!latitudeInput || !longitudeInput || !canvas) return;

        if (!window.L) {
            showStatus(status, 'Không tải được thư viện bản đồ. Bạn vẫn có thể nhập tọa độ thủ công.', true);
            return;
        }

        const initialLatitude = Number.parseFloat(latitudeInput.value);
        const initialLongitude = Number.parseFloat(longitudeInput.value);
        const picker = createMap(canvas, initialLatitude, initialLongitude, true);

        function coordinatesFromInputs() {
            const latitude = Number.parseFloat(latitudeInput.value);
            const longitude = Number.parseFloat(longitudeInput.value);
            if (!Number.isFinite(latitude) || !Number.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                if (latitudeInput.value || longitudeInput.value) {
                    showStatus(status, 'Nhập cặp tọa độ hợp lệ: vĩ độ -90 đến 90, kinh độ -180 đến 180.', true);
                }
                return null;
            }
            return [latitude, longitude];
        }

        function syncMarkerFromInputs() {
            const coordinates = coordinatesFromInputs();
            if (!coordinates) return;
            const marker = picker.setMarker(...coordinates, true);
            marker.on('dragend', updateInputsFromMarker);
            picker.map.setView(coordinates, Math.max(picker.map.getZoom(), 15));
            showStatus(status, `Đã ghim: ${coordinates[0].toFixed(6)}, ${coordinates[1].toFixed(6)}.`);
        }

        function updateInputsFromMarker(event) {
            const coordinates = event.target.getLatLng();
            latitudeInput.value = coordinates.lat.toFixed(7);
            longitudeInput.value = coordinates.lng.toFixed(7);
            latitudeInput.dispatchEvent(new Event('input', { bubbles: true }));
            longitudeInput.dispatchEvent(new Event('input', { bubbles: true }));
            showStatus(status, `Đã ghim: ${coordinates.lat.toFixed(6)}, ${coordinates.lng.toFixed(6)}.`);
        }

        if (picker.getMarker()) picker.getMarker().on('dragend', updateInputsFromMarker);
        picker.map.on('click', event => {
            latitudeInput.value = event.latlng.lat.toFixed(7);
            longitudeInput.value = event.latlng.lng.toFixed(7);
            const marker = picker.setMarker(event.latlng.lat, event.latlng.lng, true);
            marker.on('dragend', updateInputsFromMarker);
            latitudeInput.dispatchEvent(new Event('input', { bubbles: true }));
            longitudeInput.dispatchEvent(new Event('input', { bubbles: true }));
            showStatus(status, `Đã ghim: ${event.latlng.lat.toFixed(6)}, ${event.latlng.lng.toFixed(6)}.`);
        });

        latitudeInput.addEventListener('change', syncMarkerFromInputs);
        longitudeInput.addEventListener('change', syncMarkerFromInputs);

        const clearButton = element.closest('.map-picker-section')?.querySelector('[data-map-clear]');
        if (clearButton) {
            clearButton.addEventListener('click', () => {
                latitudeInput.value = '';
                longitudeInput.value = '';
                if (picker.getMarker()) picker.getMarker().remove();
                showStatus(status, 'Đã xóa ghim bản đồ.');
            });
        }

        picker.tileLayer.on('tileerror', () => showStatus(status,
            'Không tải được một số ô bản đồ. Kiểm tra kết nối Internet hoặc tiếp tục nhập tọa độ.', true));
        if (Number.isFinite(initialLatitude) && Number.isFinite(initialLongitude)) {
            showStatus(status, `Đã lưu: ${initialLatitude.toFixed(6)}, ${initialLongitude.toFixed(6)}.`);
        }
    }

    function initializeView(element) {
        const latitude = Number.parseFloat(element.dataset.latitude);
        const longitude = Number.parseFloat(element.dataset.longitude);
        const canvas = element.querySelector('[data-map-view-canvas]');
        const status = element.querySelector('[data-map-view-status]');
        if (!canvas || !Number.isFinite(latitude) || !Number.isFinite(longitude)) return;
        if (!window.L) {
            showStatus(status, 'Không tải được thư viện bản đồ. Bạn vẫn có thể mở bản đồ ngoài.', true);
            return;
        }
        const viewer = createMap(canvas, latitude, longitude, false);
        viewer.map.dragging.disable();
        viewer.map.touchZoom.disable();
        viewer.map.doubleClickZoom.disable();
        viewer.map.boxZoom.disable();
        viewer.map.keyboard.disable();
        if (viewer.map.tap) viewer.map.tap.disable();
        viewer.map.scrollWheelZoom.disable();
        showStatus(status, `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`);
        viewer.tileLayer.on('tileerror', () => showStatus(status, 'Không tải được một số ô bản đồ.', true));
    }

    document.querySelectorAll('[data-map-picker]').forEach(initializePicker);
    document.querySelectorAll('[data-map-view]').forEach(initializeView);
})();
