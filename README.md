# REAL CITY RAM V2 — Android

Projeto Android nativo independente para visualização demonstrativa, treinamento de mira local e diagnóstico do próprio aplicativo.

## V2 implementado
- Abas Visual, Aim, Player, Settings e Debug.
- Visual: 2D Box, Corner Box, Skeleton, Name/Distance, Health Bar, Tracers, Highlight, filtro de equipe simulado e filtro de distância.
- Aim Training: marcador assistido dentro do Canvas, FOV, Smoothness, distância, seleção de ponto (cabeça/peito/pernas) e Line of Sight simulada.
- Player: dados de saúde, identificação e posição de NPCs fictícios.
- Settings: vibração ao acertar, modo de baixo consumo e manter tela ativa.
- Debug: heap Java/nativo, modelo, Android e ABI.
- Preferências persistidas localmente.

## Limites
Não se conecta ao Real City, não lê memória do jogo, não acessa dados de jogadores e não injeta código. Os NPCs, distâncias, obstáculos e pontos são gerados pelo app. A integração direta exigiria SDK/API oficial do jogo.

## Compilar
1. Instale Android Studio e JDK 17 em um computador.
2. Abra esta pasta no Android Studio.
3. Aguarde a sincronização Gradle.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Instale o APK em dispositivo Android compatível.

Configuração: compileSdk 35, minSdk 23.
