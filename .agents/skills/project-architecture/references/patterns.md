# Project Architecture — Patterns Reference

## Result\<T\> Error Handling

Every Service/Repository method that can fail must return `Result<T>`. Never throw.

```dart
// ✅ GOOD
Future<Result<MemberState>> sendIdTokenToServer(String idToken);

// Consume with .when()
final result = await _authService.sendIdTokenToServer(idToken);
return result.when(
  success: (state) async => Success(state),
  failure: (msg) async => Failure(msg),
);

// ❌ BAD — raw throw forces callers into try-catch chains
Future<MemberState> sendIdTokenToServer(String idToken); // may throw
```

`Result<T>` lives in `lib/shared/base/result/result.dart`.
ViewModel **never** propagates raw exceptions to View.

---

## Navigation — Method B (View-Driven)

ViewModel changes state only. View observes via `ref.listen` and navigates.

```dart
// ✅ GOOD — ViewModel: state only
@injectable
class AuthViewModel implements AuthViewModelInterface {
  @override
  Future<void> login() async {
    final result = await _authService.login();
    result.when(
      success: (state) => _updateAuthState(state),
      failure: (msg) => _handleError(msg),
    );
  }
}

// ✅ GOOD — View: listens and navigates
@override
Widget build(BuildContext context, WidgetRef ref) {
  ref.listen(authStateProvider, (_, next) {
    if (next == AuthState.loggedIn) context.go('/home');
    if (next == AuthState.error) _showErrorSnackBar(context);
  });
  return _buildBody(context, ref);
}

// ❌ BAD — ViewModel holds router reference
class AuthViewModel {
  final GoRouter _router;
  void login() async {
    await _authService.login();
    _router.go('/home'); // breaks layer separation
  }
}
```

---

## DTO / Domain Separation

Service is the boundary. DTO never crosses into ViewModel or View.

```
Dio (JSON) → DTO (service/dto/) → Domain Model (model/) → ViewModel → View
```

```dart
// ✅ GOOD — DTO stays inside Service
@injectable
class AuthService implements AuthServiceInterface {
  @override
  Future<MemberState> sendIdTokenToServer(String idToken) async {
    final dto = await _dio.post<LoginResponseDto>(...);
    return dto.toDomain(); // converted here, DTO never escapes
  }
}

// ❌ BAD — DTO leaks into ViewModel
class AuthViewModel {
  Future<LoginResponseDto> login() async { ... }
}
```
