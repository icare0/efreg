import 'package:geolocator/geolocator.dart';

/// Service de géolocalisation en temps réel
/// Gère les permissions et la récupération de la position GPS
class LocationService {
  /// Vérifie si les services de localisation sont activés
  /// Retourne true si activés, false sinon
  Future<bool> isLocationServiceEnabled() async {
    return await Geolocator.isLocationServiceEnabled();
  }

  /// Vérifie et demande les permissions de localisation
  /// Retourne true si accordées, false sinon
  Future<bool> checkAndRequestPermissions() async {
    LocationPermission permission = await Geolocator.checkPermission();

    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return false;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      return false;
    }

    return true;
  }

  /// Récupère la position actuelle de l'appareil
  /// Throws Exception si les permissions ne sont pas accordées
  Future<Position> getCurrentPosition() async {
    final hasPermission = await checkAndRequestPermissions();
    if (!hasPermission) {
      throw Exception('Permissions de localisation refusées');
    }

    final serviceEnabled = await isLocationServiceEnabled();
    if (!serviceEnabled) {
      throw Exception('Services de localisation désactivés');
    }

    return await Geolocator.getCurrentPosition(
      desiredAccuracy: LocationAccuracy.high,
    );
  }

  /// Écoute les changements de position en temps réel
  /// Retourne un Stream de positions
  Stream<Position> getPositionStream() {
    const LocationSettings locationSettings = LocationSettings(
      accuracy: LocationAccuracy.high,
      distanceFilter: 10, // Mise à jour tous les 10 mètres
    );

    return Geolocator.getPositionStream(locationSettings: locationSettings);
  }

  /// Calcule la distance entre deux positions en mètres
  double calculateDistance(
    double startLatitude,
    double startLongitude,
    double endLatitude,
    double endLongitude,
  ) {
    return Geolocator.distanceBetween(
      startLatitude,
      startLongitude,
      endLatitude,
      endLongitude,
    );
  }
}
