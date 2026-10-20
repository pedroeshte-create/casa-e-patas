# Casa e Patas

App Android (Kotlin + Jetpack Compose) para tarefas domésticas e cuidados com animais.

- **Telemóvel (Pixel 7):** uma coluna + barra de navegação em baixo.
- **Fold 7 aberto:** a partir de 600 dp de largura passa a barra lateral + dois painéis (lista e detalhe). A atividade não é recriada ao abrir/fechar.
- **Notificações:** lembretes por tarefa, "hora de dar de comer" e "limpar as areias" (só avisam se ainda falta fazer).
- Dados guardados localmente no telemóvel (sem conta, sem internet).

## Obter o APK

1. Cria um repositório no GitHub e envia este projeto para o ramo `main`.
2. Separador **Actions** > **Build APK** > execução mais recente > **Artifacts** > `casa-e-patas-apk`.
3. Descomprime e instala `app-debug.apk` no telemóvel (permite "instalar apps desconhecidas" para o navegador/ficheiros).

Em alternativa, abre a pasta no Android Studio e usa **Build > Build APK(s)**.

## Testar as notificações no telemóvel

1. Abre a app e aceita o pedido de notificações.
2. **Definições > Enviar notificação agora**: deve aparecer logo.
3. **Definições > Testar lembrete agendado (1 minuto)**: fecha a app; a notificação chega ao fim de ~1 min (testa o alarme do sistema).
4. Em **Definições > Alarmes exatos**, toca em "Permitir" se aparecer "Não permitidos".
5. Cria uma tarefa com lembrete daqui a 2 minutos e fecha a app.

O workflow também corre testes automáticos de notificações num emulador Android 14.
