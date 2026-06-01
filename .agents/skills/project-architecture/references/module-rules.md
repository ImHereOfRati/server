# Project Architecture — Module Rules Reference

## Module-Specific Rules

| Module | Key Rule |
|--------|----------|
| `feature/auth` | Global auth state via `auth_state_provider.dart`. Tokens in `SecureStorage` only. |
| `feature/geofence` | `geofence_orchestrator.dart` is the single controller for location + notifications. |
| `feature/user_permission` | Location, SMS, FCM, Contact permissions managed independently per-type. |
| `integration/fcm` | FCM token lifecycle (generate → store → enroll) via `FcmTokenService` only. |
| `integration/firebase` | Crashlytics access only through `lib/integration/firebase/` services. |

## Interface-Driven Design Rule

Every Service and ViewModel must have a `*_interface.dart`:

```dart
// ✅ GOOD
// lib/feature/auth/service/auth_service_interface.dart
abstract class AuthServiceInterface {
  Future<MemberState> sendIdTokenToServer(String idToken);
}

// lib/feature/auth/service/auth_service.dart
@injectable
class AuthService implements AuthServiceInterface { ... }

// ❌ BAD — concrete class without interface
@injectable
class AuthService {
  Future<MemberState> sendIdTokenToServer(String idToken) { ... }
}
```

## DI Rules

```dart
// ✅ GOOD — GetIt injection
@injectable
class AuthViewModel implements AuthViewModelInterface {
  final AuthService _authService;
  AuthViewModel(this._authService);
}

// ❌ BAD — manual instantiation
final vm = AuthViewModel(AuthService(Dio()));
```

Responsive UI — every size via `flutter_screenutil`:

```dart
// ✅
SizedBox(width: 20.w, height: 10.h)
Text('x', style: TextStyle(fontSize: 15.sp))
BorderRadius.circular(5.r)

// ❌
SizedBox(width: 20, height: 10)
```
