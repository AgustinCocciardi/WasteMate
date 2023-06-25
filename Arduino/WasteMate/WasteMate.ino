#include <PWMServo.h>
#include <Wire.h>
#include <SoftwareSerial.h>
#include "rgb_lcd.h"
#include <ArduinoJson.h>

//Solo habilitamos los logs de debug en caso de ser necesario, para evitar carga extra en la versión en producción.
#define SERIAL_DEBUG_ENABLED 1

#if SERIAL_DEBUG_ENABLED
  #define debug_print(message)\
  {\
    Serial.print(message);\
  }
  #define debug_println(message)\
  {\
    Serial.println(message);\
  }
#else
  #define debug_print(message)
  #define debug_println(message)
#endif

//SENSORES
#define PIR_SENSOR 8
#define FLEX_SENSOR A0
#define ULTRASONIC_SENSOR_TRIGGER 7
#define ULTRASONIC_SENSOR_ECHO 6

//ACTUADORES
#define SERVOMOTOR 9

//CONSTANTES
#define GENERAL_TIMEOUT_LIMIT 50
#define PRESENCE_TIMEOUT_LIMIT 5000
#define MINIMUM_STATE_INDEX 0
#define MAXIMUM_STATE_INDEX 3
#define MINIMUM_EVENT_INDEX 0
#define MAXIMUM_EVENT_INDEX 9
#define MAXIMUM_INDEX_VERIFICATIONS 4
#define DETECTION_TIMES 10
#define _0_DEGREES 0
#define _90_DEGREES 90
#define _180_DEGREES 180
#define MAXIMUM_WEIGHT_ALLOWED 500
#define CRITICAL_DISTANCE 40
#define MINIMUM_ALLOWED_DISTANCE 15
#define LCD_MEMORY 32
#define LCD_COLUMNS 16
#define LCD_ROWS 2

//Obtenemos el valor 0.01715 de la siguiente manera:
//La velocidad del sonido es 0.0343 centimetros por microsegundo.
//Lo dividimos por 2 porque solo nos interesa la mitad del tiempo que tarda en ir y volver la onda de sonido.
//AL multiplicar este valor por el tiempo leido en el sensor, obtenemos la distancia en centímetros.
#define PULSE_DURATION_TO_DISTANCE_FACTOR 0.01715 

//Definimos una zona muerta para que alcanzar el valor crítico de distancia no haga transiciones constantes por fluctuaciones en la lectura de los sensores.
#define DEADBAND 10

//Utilizamos un pulso disparador de 10 microsegundos para el correcto funcionamiento del sensor ultrasonido porque es lo recomendado.
#define TRIGGER_PULSE 10

//MENSAJES
#define MESSAGE_MAINTENANCE_NEEDED        "EL TACHO NECESITA MANTENIMIENTO"
#define MESSAGE_REQUEST_DISABLING         "TACHO FUERA DE SERVICIO"
#define MESSAGE_MAINTENANCE               "EL TACHO ESTA EN MANTENIMIENTO"
#define MESSAGE_MAINTENANCE_FINISHED      "MANTENIMIENTO TERMINADO"
#define MESSAGE_ERROR                     "LA OPERACION NO ES VALIDA PARA EL ESTADO ACTUAL"

//COMANDOS
#define CMD_START_MAINTENANCE     "0"
#define CMD_MAINTENANCE_FINISHED  "1"
#define CMD_DISABLE               "2"
#define CMD_CONFIGURE_THRESHOLDS  "3"
//TIPOS DE DATOS
typedef enum status
{ 
  ST_UNF,       //STATUS UNFILLED
  ST_CRIT_CAP,  //STATUS CRITICAL CAPACITY
  ST_NO_CAP,    //STATUS NO CAPACITY
  ST_MAINT      //STATUS MAINTENANCE
} t_status;

typedef enum event
{
  EV_CONT,    //EVENT CONTINUE
  EV_NPD,     //EVENT NO PRESENCE DETECTED
  EV_PD,      //EVENT PRESENCE DETECTED
  EV_MAX_WR,  //EVENT MAX WEIGHT REACHED
  EV_UNF,     //EVENT UNFILLED
  EV_CCR,     //EVENT CRITICAL CAPACITY REACHED
  EV_MCR,     //EVENT MAXIMUM CAPACITY REACHED
  EV_SM,      //EVENT SEND TO MAINTENANCE / START MAINTENANCE?
  EV_MF,      //EVENT MAINTENANCE FINISHED
  EV_DIS,     //EVENT DISABLED
} t_event;

typedef void (*t_actions)();

typedef struct st_timer
{
  unsigned long previous_time;
  unsigned long current_time;
  bool timeout;
  unsigned long limit;
} t_timer;

typedef bool (*t_verification)();

//FUNCIONES DE TRANSICION
void none();
void open();
void close();
void disable();
void send_maintenance();
void request_disabling();
void reset();
void error();
void notify_state();

//FUNCIONES PARA VERIFICAR EVENTOS
bool verify_presence();
bool verify_weight();
bool verify_message();
bool verify_capacity();

//FUNCIONES GENERALES
bool timeout_reached(t_timer *timer);
void reset_timer(t_timer *timer);
void move_servomotor(byte degrees);
void send_notification(const char* message);
void initialize();
long get_echo(int pin);
double get_distance(int pin);
void show_status(t_status status);
void log_status();
void log(const char* message);
void calibrate_pir();
void calibrate_flex_sensor();

//VARIABLES GLOBALES
const int colorR = 255;
const int colorG = 0;
const int colorB = 0;

PWMServo servo;
rgb_lcd lcd;
SoftwareSerial bluetooth_serial(10,11); // RX | TX

t_timer timer_general;
t_timer timer_presence;
t_status current_state;
t_status previous_state;
t_event current_event;

int is_open = 0;
int presence_detection_counter = 0;
int critical_distance = CRITICAL_DISTANCE;
int minimum_allowed_distance = MINIMUM_ALLOWED_DISTANCE;
int maximum_weight_allowed = MAXIMUM_WEIGHT_ALLOWED;

t_actions action[MAXIMUM_STATE_INDEX + 1][MAXIMUM_EVENT_INDEX + 1] =
{
  { none         , close     , open     , disable        , none      , none      , disable     , error             , none     , error             },    //ST_UNF
  { none         , close     , open     , disable        , none      , none      , disable     , error             , error    , request_disabling },    //ST_CRIT_CAP
  { none         , none      , none     , none           , none      , none      , none        , send_maintenance  , error    , none              },    //ST_NO_CAP
  { none         , none      , none     , none           , none      , none      , none        , error             , reset    , error             },    //ST_MAINT
  //EV_CONT        EV_NPD      EV_PD      EV_MAX_WR      EV_UNF      EV_CCR      EV_MCR      EV_SM               EV_MF      EV_DIS 
};


t_status transition[MAXIMUM_STATE_INDEX + 1][MAXIMUM_EVENT_INDEX + 1] =
{
  { ST_UNF       , ST_UNF         , ST_UNF         , ST_NO_CAP    , ST_UNF       , ST_CRIT_CAP    , ST_NO_CAP    , ST_UNF         , ST_UNF         , ST_UNF    },    //ST_UNF
  { ST_CRIT_CAP  , ST_CRIT_CAP    , ST_CRIT_CAP    , ST_NO_CAP    , ST_UNF       , ST_CRIT_CAP    , ST_NO_CAP    , ST_CRIT_CAP    , ST_CRIT_CAP    , ST_NO_CAP },    //ST_CRIT_CAP
  { ST_NO_CAP    , ST_NO_CAP      , ST_NO_CAP      , ST_NO_CAP    , ST_NO_CAP    , ST_NO_CAP      , ST_NO_CAP    , ST_MAINT       , ST_NO_CAP      , ST_NO_CAP },    //ST_NO_CAP
  { ST_MAINT     , ST_MAINT       , ST_MAINT       , ST_MAINT     , ST_MAINT     , ST_MAINT       , ST_MAINT     , ST_MAINT       , ST_UNF         , ST_MAINT  },    //ST_MAINT
  //EV_CONT        EV_NPD           EV_PD            EV_MAX_WR        EV_UNF         EV_CCR           EV_MCR         EV_SM            EV_MF            EV_DIS 
};

const char* STATUS_DESCRIPTION [] = 
{
  "CON CAPACIDAD",
  "CAPACIDAD CRITICA",
  "SIN CAPACIDAD",
  "MANTENIMIENTO"
};

const char* EVENT_DESCRIPTION [] = 
{
  "CONTINUAR",
  "SIN PRESENCIA",
  "PRESENCIA DETECTADA",
  "PESO MAXIMO ALCANZADO",
  "CAPACIDAD DISPONIBLE",
  "CAPACIDAD CRITICA ALCANZADA",
  "CAPACIDAD MAXIMA ALCANZADA",
  "ENVIAR A MANTENIMIENTO",
  "MANTENIMIENTO TERMINADO",
  "DESHABILITAR TACHO"
};

t_verification verification[MAXIMUM_INDEX_VERIFICATIONS] = 
{
  verify_presence,
  verify_weight,
  verify_message,
  verify_capacity
};

byte index_verification;

//INICIALIZACION

void setup()
{
  Serial.begin(9600);
  bluetooth_serial.begin(9600); 

  // set up the LCD's number of columns and rows:
  lcd.begin(16, 2);
  lcd.setRGB(colorR, colorG, colorB);
  
  pinMode(PIR_SENSOR, INPUT);
  pinMode(FLEX_SENSOR, INPUT);
  pinMode(ULTRASONIC_SENSOR_TRIGGER, INPUT);
  servo.attach(SERVOMOTOR);  
  
  lcd.print("CALIBRANDO...");
  calibrate_pir();
  calibrate_flex_sensor();

  
  timer_general.limit = GENERAL_TIMEOUT_LIMIT;
  timer_presence.limit = PRESENCE_TIMEOUT_LIMIT;
  initialize();
  current_state = ST_UNF;
  previous_state = ST_UNF;
  show_status(current_state);
  log_status();
}

//MAQUINA DE ESTADO

void loop()
{
  get_event();
  if (current_event >= MINIMUM_EVENT_INDEX && current_event <= MAXIMUM_EVENT_INDEX && current_state >= MINIMUM_STATE_INDEX && current_state <= MAXIMUM_STATE_INDEX)
  {
    previous_state = current_state;
    action[current_state][current_event]();
    current_state = transition[current_state][current_event];
    if(previous_state != current_state)
    {
      show_status(current_state);
      notify_state();
      log_status();
    }
  }
  else
  {
    Serial.println("ERROR: evento desconocido");
  }
}

//DEFINICION DE FUNCIONES

void get_event()
{   
  if(timeout_reached(&timer_general))
  {
    reset_timer(&timer_general);
    int index = (index_verification % MAXIMUM_INDEX_VERIFICATIONS);
    index_verification++;
    verification[index]();
  }
  else
  {
    current_event = EV_CONT;
  }
}

//Verifica si se detecta movimiento con el sensor PIR.
//Si no se detecta, verifica si se superó el tiempo de tolerancia. Si aún no expiró, lo considera como que aún hay presencia detectada.
bool verify_presence()
{
  int value = digitalRead(PIR_SENSOR);
  if (value == HIGH)
  {
    presence_detection_counter++;
    if(presence_detection_counter >= DETECTION_TIMES)
    {
        reset_timer(&timer_presence);
        current_event = EV_PD;
        presence_detection_counter = 0;
    }   
    return true;
  }
  else
  {
    if(timer_presence.current_time > 0 && timeout_reached(&timer_presence))
    {
      current_event = EV_NPD;
    }
    return false;
  }
}

//Verifica si el peso excede los valores permitidos. 
//En este caso, mapeamos los valores leídos del sensor flex a los grados que puede flexionarse.
//0 - Nada flexionado,  1 - Completamente flexionado.
bool verify_weight()
{
  int flex_value = analogRead(FLEX_SENSOR);              
  if(flex_value >= maximum_weight_allowed)
  {
    current_event = EV_MAX_WR;
    return true;
  } 
  return false;
}

//Simulación de la detección de órdenes por medio de la aplicación.
//Se reemplazará por la implementación real.
bool verify_message()
{
  //int read = 0;
  bluetooth_serial.flush();
  if(bluetooth_serial.available())
  {
    //se los lee y se los muestra en el monitor serie
    String read = bluetooth_serial.readString();
    DynamicJsonDocument doc(60);

    bluetooth_serial.flush();
    read.trim();
    Serial.println(read);

    DeserializationError error = deserializeJson(doc, read);
    if (error) {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

  String code = doc["c"];
  Serial.println(code);
    if(code == CMD_START_MAINTENANCE){
      current_event = EV_SM;
    return true;
    }
    else if(code == CMD_DISABLE){
      current_event = EV_DIS;
      return true;
    }
    else if(code == CMD_MAINTENANCE_FINISHED){
      current_event = EV_MF;
      return true;
    }
    else if(code == CMD_CONFIGURE_THRESHOLDS){
      minimum_allowed_distance = doc["d"]["md"];
      maximum_weight_allowed =  doc["d"]["mw"];;
    }
  }
}

//Verifica el volumen ocupado.
bool verify_capacity()
{
  double distance = get_distance(ULTRASONIC_SENSOR_TRIGGER);
  if(distance < minimum_allowed_distance)
  { 
    current_event = EV_MCR;
  }
  else
  {
    //Agregamos una franja de tolerancia, ya que sin ella teníamos transiciones constantes en el umbral por variaciones de lectura del sensor.
    if(distance < critical_distance - DEADBAND)
    {
      current_event = EV_CCR ;
    }
    else if (distance >= critical_distance + DEADBAND)
    {
      current_event = EV_UNF;
    }
  }
  return true;
}

//Verifica si el timer alcanzó el timeout.
bool timeout_reached(t_timer *timer)
{
  timer->current_time = millis();
  unsigned long difference = (timer->current_time) - (timer->previous_time); 
  timer->timeout = (difference > timer->limit) ? true : false;
  return timer->timeout;
}

//Reinicia el timer
void reset_timer(t_timer *timer)
{
  timer->timeout = false;
  timer->previous_time = millis();
  timer->current_time = millis();
}

void none() 
{
}

//Abre la tapa del contenedor
void open() 
{
  if(!is_open)
  {
    move_servomotor(_180_DEGREES);
    is_open = 1;
  }
}

//Cierra la tapa del contenedor
void close()
{
  if(is_open)
  {
    move_servomotor(_90_DEGREES);
    is_open = 0;
  }
}

//Deshabilita el contenedor y notifica que se necesita mantenimiento
void disable()
{
  //send_notification(MESSAGE_MAINTENANCE_NEEDED); //TODO:: VER SI ES NECESARIO
  close();
}

//Deshabilita el contenedor y notifica que se solicitó deshabilitarlo.
void request_disabling()
{
  //send_notification(MESSAGE_REQUEST_DISABLING); //TODO: VER SI ES NECESARIO
  disable();
}

//Notifica que se inició el mantenimiento del contenedor.
void send_maintenance()
{
  //send_notification(MESSAGE_MAINTENANCE); //TODO: VER SI ES NECESARIO.
}

//Reinicia el sistema al estado inicial.
void reset()
{
  //send_notification(MESSAGE_MAINTENANCE_FINISHED); //TODO: VER SI ES NECESARIO.
  initialize();
  close();
}

//Inicializa el sistema.
void initialize()
{
  reset_timer(&timer_general);
  reset_timer(&timer_presence);  
  index_verification = 0;
}

void move_servomotor(byte degrees)
{
  servo.write(degrees);
}

//Obtiene la distancia en centímetros utilizando el factor de conversión.
double get_distance(int pin)
{
	return PULSE_DURATION_TO_DISTANCE_FACTOR * get_echo(pin);
}

//Obtiene el ancho de pulso del sensor.
long get_echo(int pin)
{
  pinMode(pin, OUTPUT);
  digitalWrite(pin, LOW);
  delayMicroseconds(2);
  digitalWrite(pin, HIGH);
  delayMicroseconds(TRIGGER_PULSE);
  digitalWrite(pin, LOW);
  pinMode(ULTRASONIC_SENSOR_ECHO, INPUT);
  return pulseIn(ULTRASONIC_SENSOR_ECHO, HIGH);
}

//Imprime el estado en la pantalla.
void show_status(t_status status)
{
  lcd.clear();

  String message = STATUS_DESCRIPTION[status];
  if(message.length() > LCD_COLUMNS) 
  {
    byte space = message.indexOf(' ');
    if (space != -1)
    {
      lcd.print(message.substring(0, space));
      lcd.setCursor(0, 1);
      lcd.print(message.substring(space + 1));
      return;
    }
  }
  lcd.print(message);
}

//Notifica el estado actual.
void notify_state()
{
  send_notification(STATUS_DESCRIPTION[current_state]);
}

//Notifica la ocurrencia de un error.
void error()
{
  send_notification(MESSAGE_ERROR);
}

//Envía una notificación a la aplicación.
void send_notification(const char* message)
{
  bluetooth_serial.println(message); 
}

//Log de los estados.
void log_status()
{
  debug_print("DEBUG: EVENTO ");
  debug_println(EVENT_DESCRIPTION[current_event]);
  debug_print("DEBUG: ESTADO ");
  debug_println(STATUS_DESCRIPTION[current_state]);
}

void log(const char* message)
{
  debug_print("DEBUG: ");
  debug_println(message);
}

void calibrate_pir()
{
  char is_ok = 'n';
  do
  {
    int value = digitalRead(PIR_SENSOR);
    if (value == HIGH)
    {
      Serial.println("Presence");
    }
    else
    {
      Serial.println("No Presence");
    }
    Serial.println("Ingrese 'y' cuando detecte que el sensor se estabilizo y no detecta falsos positivos.");
    if(Serial.available())
    {
      is_ok = Serial.read();
    }
    delay(1000);
  } while (is_ok != 'y');
}

void calibrate_flex_sensor()
{
 char is_ok = 'n';
  do
  {
    if(Serial.available())
    {
      is_ok = Serial.read();
    }
    delay(1000);
    Serial.println("Ingrese 'y' haya colocado el peso limite.");

  } while (is_ok != 'y'); 
  maximum_weight_allowed = analogRead(FLEX_SENSOR);       
  is_ok = 'n';
  do
  {
    if(Serial.available())
    {
      is_ok = Serial.read();
    }
    delay(1000);
    Serial.println("Ingrese 'y' haya quitado el peso.");

  } while (is_ok != 'y'); 
}