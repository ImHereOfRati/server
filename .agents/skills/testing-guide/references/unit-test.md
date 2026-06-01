# Testing Guide — Unit Test Reference

## Full Unit Test Example

```dart
// test/feature/auth/auth_view_model_test.dart
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/annotations.dart';
import 'package:mockito/mockito.dart';
import 'package:iamhere/feature/auth/view_model/auth_view_model.dart';
import 'package:iamhere/feature/auth/service/auth_service_interface.dart';
import 'package:iamhere/integration/fcm/service/fcm_token_service.dart';
import 'package:iamhere/shared/base/result/result.dart';
import 'auth_view_model_test.mocks.dart';

@GenerateMocks([AuthServiceInterface, FcmTokenService])
void main() {
  late MockAuthServiceInterface mockAuthService;
  late MockFcmTokenService mockFcmTokenService;
  late AuthViewModel sut; // System Under Test

  setUp(() {
    mockAuthService = MockAuthServiceInterface();
    mockFcmTokenService = MockFcmTokenService();
    sut = AuthViewModel(mockAuthService, mockFcmTokenService);
  });

  group('handleKakaoLogin', () {
    test('success_existingUser_returnsSuccessWithExistingUser', () async {
      // Arrange
      when(mockAuthService.sendIdTokenToServer(any))
          .thenAnswer((_) async => MemberState.existingUser);

      // Act
      final result = await sut.handleKakaoLogin();

      // Assert
      expect(result, isA<Success<MemberState>>());
      final success = result as Success<MemberState>;
      expect(success.value, MemberState.existingUser);
    });

    test('serviceThrows_returnsFailure', () async {
      // Arrange
      when(mockAuthService.sendIdTokenToServer(any))
          .thenThrow(Exception('network error'));

      // Act
      final result = await sut.handleKakaoLogin();

      // Assert
      expect(result, isA<Failure<MemberState>>());
    });
  });

  group('requestFCMTokenAndSendToServer', () {
    test('nullToken_returnsFailure', () async {
      when(mockFcmTokenService.generateAndSaveFcmToken())
          .thenAnswer((_) async => null);

      final result = await sut.requestFCMTokenAndSendToServer();

      expect(result, isA<Failure>());
    });
  });
}
```

## setUp / tearDown Pattern

```dart
setUp(() {
  // runs before each test — initialize mocks and SUT
});

tearDown(() {
  // runs after each test — cleanup if needed
});

setUpAll(() {
  // runs once before all tests in group
});
```

## Verifying Mock Calls

```dart
// Verify method was called exactly once
verify(mockAuthService.sendIdTokenToServer(any)).called(1);

// Verify method was never called
verifyNever(mockFcmTokenService.enrollFcmTokenToServer());

// Capture arguments
final captured = verify(mockAuthService.sendIdTokenToServer(captureAny));
expect(captured.captured.first, 'expected-token');
```
