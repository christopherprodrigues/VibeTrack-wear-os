# VibeTrack Wear OS (Smartwatch)

Este repositório contém o código-fonte da aplicação para relógios inteligentes (**Wear OS**) do projeto **VibeTrack**. O foco desta aplicação é a coleta eficiente de sinais vitais utilizando os sensores de hardware do dispositivo.

Desenvolvido em **Kotlin**, o projeto utiliza a moderna interface declarativa **Jetpack Compose para Wear OS**.

## Funcionalidades Principais

* **Coleta de Sensores:** Acesso direto aos sensores de hardware para monitoramento de:
    * Frequência Cardíaca (`Sensor.TYPE_HEART_RATE`).
    * Contagem de Passos (`Sensor.TYPE_STEP_COUNTER`).
* **Processamento Local:** Cálculo de métricas (média, repouso e máximo) diretamente no dispositivo antes da transmissão.
* **Transmissão de Dados:** Envio de payloads JSON para o celular pareado utilizando a `MessageClient API` do Google.
* **Feedback Visual:** Interface simples e objetiva para iniciar/parar a coleta e visualizar o status dos sensores.

## Arquitetura e Tecnologias

A aplicação segue a arquitetura **MVVM (Model-View-ViewModel)** e utiliza as tecnologias mais recentes para desenvolvimento em wearables:

* **Linguagem:** Kotlin.
* **UI Toolkit:** Jetpack Compose for Wear OS (Material).
* **Gerenciamento de Estado:** Kotlin Coroutines & StateFlow.
* **Android Sensors API:** `SensorManager` e `SensorEventListener` para leitura bruta de dados.
* **Standalone Mode:** Configurado como `standalone=true`, mas projetado para operar em conjunto com o app mobile.

### Estrutura do Projeto

* `presentation/WearViewModel.kt`: Contém toda a lógica de negócio, incluindo a inicialização de sensores, coleta de dados em listas temporárias e o envio via Bluetooth.
* `presentation/MainActivity.kt`: Implementação da interface do usuário utilizando componentes Compose (`Scaffold`, `Vignette`, `TimeText`).
* `data/model/`: Classes de dados espelhadas do projeto mobile para garantir consistência na serialização JSON.

## Como Executar

1.  Abra este projeto no **Android Studio**.
2.  Selecione o módulo `vibetrackwearos` na configuração de execução.
3.  Execute em um emulador Wear OS ou em um relógio físico (com modo desenvolvedor ativado).
4.  **Permissões:** Ao iniciar, conceda as permissões de "Sensores Corporais" e "Atividade Física" para garantir o funcionamento da coleta.
