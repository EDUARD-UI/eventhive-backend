package com.eventhive.app.auto;

public enum VeredictoModeracion {
    APROBADO,   // cumple todo -> se publica sin intervención humana
    REVISION,   // caso dudoso -> pasa a la bandeja del moderador humano
    RECHAZADO   // incumple una regla dura -> se rechaza automáticamente
}