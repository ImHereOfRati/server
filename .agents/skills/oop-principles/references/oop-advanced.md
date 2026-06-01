# OOP Principles — Advanced Details

## 5. Single Responsibility Principle (SRP)

**Why:** 클래스나 메서드가 커질수록(God Class) 결합도가 높아져 변경 시 부작용이 커집니다. 변경해야 하는 이유는 단 하나여야 합니다.

**심화 예시:**
```dart
// ❌ BAD — 3가지 책임(인증, 알림, 결제)을 모두 가짐
class AppViewModel {
  void login() { ... }
  void subscribeTopic() { ... }
  void processPayment() { ... }
}

// ✅ GOOD — 책임별로 클래스 분리
class AuthViewModel { ... }
class FcmViewModel { ... }
class PaymentViewModel { ... }
```

## 6. Dependency Inversion Principle (DIP)

**Why:** 구체적인 구현체에 의존하면 모듈 교체나 Mocking(테스트)이 불가능해집니다. 항상 추상화(Interface)에 의존해야 합니다.

**심화 예시:**
```dart
// ❌ BAD — 구현체 의존
class AuthViewModel {
  final KakaoAuthServiceImpl _authService; // 구체 클래스
  AuthViewModel(this._authService);
}

// ✅ GOOD — 인터페이스 의존
class AuthViewModel {
  final AuthServiceInterface _authService; // 추상화
  AuthViewModel(this._authService);
}
```

## 7. Composition over Inheritance (상속보다는 합성)

**Why:** 상속(`extends`)은 부모 클래스의 내부 구현에 자식 클래스를 강하게 결합시킵니다(White-box reuse). 합성은 인터페이스를 통해 느슨하게 결합합니다(Black-box reuse). Flutter 위젯 밖의 비즈니스 로직에서는 상속을 피하세요.

**심화 예시:**
```dart
// ❌ BAD — 상속을 통한 로직 재사용
class PremiumUser extends BaseUser {
  void doPremiumAction() {
    super.baseAction(); // 부모 내부에 종속됨
  }
}

// ✅ GOOD — 합성을 통한 로직 재사용
class PremiumUser {
  final UserRoleBehavior _behavior; // 행동을 객체로 주입(합성)
  PremiumUser(this._behavior);

  void doPremiumAction() {
    _behavior.execute();
  }
}
```

## 8. Collection Encapsulation (일급 컬렉션)

**Why:** 도메인 모델 내부의 `List`를 그대로 노출하면, 외부 객체가 마음대로 `.add()`나 `.remove()`를 호출할 수 있어 상태 관리가 깨집니다.

**심화 예시:**
```dart
// ❌ BAD — 내부 리스트 노출
class Cart {
  final List<Item> items = [];
  // 외부에서 cart.items.add(newItem) 가능
}

// ✅ GOOD — 불변 리스트로 래핑하여 노출
class Cart {
  final List<Item> _items = [];
  
  // 조회용 (수정 불가)
  List<Item> get items => List.unmodifiable(_items);
  
  // 수정은 오직 객체 내부 메서드를 통해서만
  void addItem(Item item) {
    _items.add(item);
  }
}
```
