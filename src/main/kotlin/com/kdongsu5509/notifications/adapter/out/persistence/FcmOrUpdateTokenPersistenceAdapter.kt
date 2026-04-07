package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.application.port.out.DeleteTokenPort
import com.kdongsu5509.notifications.application.port.out.FindTokenPort
import com.kdongsu5509.notifications.application.port.out.SaveOrUpdateTokenPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import org.springframework.stereotype.Component

@Component
class FcmOrUpdateTokenPersistenceAdapter(
    private val fcmTokenMapper: FcmTokenMapper,
    private val springDataFcmTokenRepository: SpringDataFcmTokenRepository,
) : SaveOrUpdateTokenPersistencePort, FindTokenPort, DeleteTokenPort {

    override fun saveOrUpdate(userEmail: String, fcmToken: String, deviceType: DeviceType) {
        val existingEntity = springDataFcmTokenRepository.findByUserEmail(userEmail)


        if (existingEntity != null) {
            updateFcmTokenIfChanged(existingEntity, fcmToken)
            return;
        }
        saveNewFcmToken(fcmToken, userEmail, deviceType)
    }

    private fun saveNewFcmToken(
        fcmToken: String,
        userEmail: String,
        deviceType: DeviceType
    ) {
        springDataFcmTokenRepository.save(
            FcmTokenJpaEntity(
                token = fcmToken,
                userEmail = userEmail,
                deviceType = deviceType
            )
        )
    }

    private fun updateFcmTokenIfChanged(
        existingEntity: FcmTokenJpaEntity,
        fcmToken: String
    ) {
        if (existingEntity.token != fcmToken) {
            existingEntity.updateToken(fcmToken)
        }
    }

    override fun findByUserEmail(userEmail: String): FcmToken? {
        return springDataFcmTokenRepository.findByUserEmail(userEmail)
            ?.let { fcmTokenMapper.mapToDomainEntity(it) }
    }

    override fun deleteById(fcmTokenId: Long) {
        springDataFcmTokenRepository.deleteById(fcmTokenId)
    }
}
