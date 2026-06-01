# Testing Guide — Widget Test Reference

## Widget Test Example

```dart
// test/feature/auth/view/auth_view_test.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:iamhere/feature/auth/view/auth_view.dart';
import 'package:iamhere/feature/auth/view_model/auth_view_model_interface.dart';
import 'package:iamhere/shared/base/result/result.dart';

void main() {
  testWidgets('loginButton_tapped_showsLoadingIndicator', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authViewModelProvider.overrideWith((_) => FakeAuthViewModel()),
        ],
        child: const MaterialApp(home: AuthView()),
      ),
    );

    await tester.tap(find.byKey(const Key('login_button')));
    await tester.pump();

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });

  testWidgets('initialState_loginButtonVisible', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          authViewModelProvider.overrideWith((_) => FakeAuthViewModel()),
        ],
        child: const MaterialApp(home: AuthView()),
      ),
    );

    expect(find.byKey(const Key('login_button')), findsOneWidget);
  });
}

// Fake ViewModel — no real network
class FakeAuthViewModel implements AuthViewModelInterface {
  @override
  Future<Result<MemberState>> handleKakaoLogin() async =>
      Success(MemberState.existingUser);

  @override
  Future<Result<ResultMessage>> requestFCMTokenAndSendToServer() async =>
      Success(ResultMessage.fcmTokenGenerateSuccess);
}
```

## Widget Test Rules

- Real network: **absolutely forbidden**. Always use Provider override or Fake.
- Every tappable widget must have a `Key` → use `find.byKey()`
- `pump()` advances one frame. `pumpAndSettle()` waits for all animations.

## Test File Structure

```
test/
├── feature/
│   ├── auth/
│   │   ├── auth_view_model_test.dart
│   │   ├── auth_view_model_test.mocks.dart  ← build_runner generated
│   │   ├── service/
│   │   │   └── auth_service_test.dart
│   │   └── view/
│   │       └── auth_view_test.dart
│   └── geofence/
│       └── ...
├── shared/
│   └── base/
│       └── result_test.dart
└── integration_test/
    └── app_test.dart
```
