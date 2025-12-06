import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_gps_calendar_poc/services/location_service.dart';

/// Tests unitaires pour LocationService
/// Vérifie les fonctionnalités de base du service de géolocalisation
void main() {
  group('LocationService Tests', () {
    late LocationService locationService;

    setUp(() {
      locationService = LocationService();
    });

    test('calculateDistance should return correct distance between two points', () {
      // Paris: 48.8566° N, 2.3522° E
      // Londres: 51.5074° N, -0.1278° W
      const parisLat = 48.8566;
      const parisLon = 2.3522;
      const londonLat = 51.5074;
      const londonLon = -0.1278;

      final distance = locationService.calculateDistance(
        parisLat,
        parisLon,
        londonLat,
        londonLon,
      );

      // La distance entre Paris et Londres est d'environ 340-350 km
      expect(distance, greaterThan(340000)); // Plus de 340 km
      expect(distance, lessThan(360000));    // Moins de 360 km
    });

    test('calculateDistance should return 0 for same coordinates', () {
      const lat = 48.8566;
      const lon = 2.3522;

      final distance = locationService.calculateDistance(lat, lon, lat, lon);

      expect(distance, equals(0.0));
    });

    test('calculateDistance should return positive value', () {
      const lat1 = 40.7128; // New York
      const lon1 = -74.0060;
      const lat2 = 34.0522; // Los Angeles
      const lon2 = -118.2437;

      final distance = locationService.calculateDistance(lat1, lon1, lat2, lon2);

      expect(distance, greaterThan(0));
    });

    test('isLocationServiceEnabled should return a boolean', () async {
      // Ce test vérifie que la méthode retourne bien un booléen
      // Note: Le résultat peut varier selon l'environnement de test
      final result = await locationService.isLocationServiceEnabled();
      expect(result, isA<bool>());
    });
  });
}
