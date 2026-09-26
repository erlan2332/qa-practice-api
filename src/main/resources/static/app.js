'use strict';
const $ = id => document.getElementById(id);
const state = {token:'', productId:'', orderId:'', email:`student-${Date.now()}@example.test`, completed:new Set(), lesson:0};
const lessons = [
 {title:'Привет, сервер',method:'GET',path:'/api/catalog',status:[200],description:'GET означает «дай посмотреть». Попроси магазин показать витрину. Регистрация пока не нужна. Запрос не меняет данные.',check:'Статус 200. В items — три товара. Найди товар с остатком stock: 0.'},
 {title:'Создай свою песочницу',method:'POST',path:'/api/auth/register',status:[201],body:{email:'{{email}}',password:'LearnOnly123!',name:'Начинающий QA'},description:'POST означает «создай». Отправь email, учебный пароль и имя в JSON. В ответ сервер выдаст токен — временный пропуск. Страница подставит его в следующие запросы.',check:'Статус 201, token и user. Повторная отправка того же email даст 409 — он уже занят.'},
 {title:'Кто я для сервера?',method:'GET',path:'/api/me',status:[200],description:'Заголовок Authorization: Bearer <token> сообщает серверу, от чьего имени пришёл запрос. Тело у этого GET не нужно.',check:'Статус 200. email совпадает с регистрацией. Пароля в ответе нет. Без токена должно быть 401.'},
 {title:'Твои товары',method:'GET',path:'/api/products?page=0&size=10',status:[200],description:'Это уже не общая витрина, а твоя копия магазина. page и size после ? — параметры запроса. Страница запомнит id первого товара для упражнений.',check:'Статус 200. total: 3. Поменяй size на 1: items станет короче, а total останется 3.'},
 {title:'Добавь новый товар',method:'POST',path:'/api/products',status:[201],body:{name:'Клавиатура QA',price:1500,stock:7},description:'Создай товар в своей песочнице. price — целое число сомов. name, price и stock обязательны. Его новый id станет текущим для следующих шагов.',check:'201 и заголовок Location. После повторного GET /api/products total станет 4.'},
 {title:'Проверь плохие данные',method:'POST',path:'/api/products',status:[400],body:{name:'',price:-1,stock:7},description:'Тестировщик проверяет не только хороший сценарий. Здесь пустое имя и отрицательная цена. Ожидаем отказ, а не создание сломанного товара.',check:'400, code: VALIDATION_ERROR. В fields указаны name и price. Товар не создался.'},
 {title:'PUT: замени целиком',method:'PUT',path:'/api/products/{{productId}}',status:[200],body:{name:'Клавиатура QA v2',price:1700,stock:8},description:'PUT заменяет все изменяемые поля товара. Здесь нужно передать и name, и price, и stock. Попробуй убрать name: полная замена станет невалидной.',check:'200. id тот же. name, price и stock изменились. Отправь тот же PUT ещё раз: итоговое состояние не изменится.'},
 {title:'PATCH: измени часть',method:'PATCH',path:'/api/products/{{productId}}',status:[200],body:{stock:5},description:'PATCH этого API меняет только остаток. Имя и цена сохраняются. Важно: конкретный контракт PATCH зависит от API, он не везде одинаковый.',check:'200 и stock: 5. name и price такие же, как после PUT.'},
 {title:'Собери заказ',method:'POST',path:'/api/orders',status:[201,200],key:'lesson-order-1',body:{productId:'{{productId}}',quantity:2},description:'Закажи две штуки. Сервер сам считает сумму и резервирует остаток. Idempotency-Key защищает от повторного создания при повторе того же запроса.',check:'Первый раз — 201. total = unitPrice × 2. Повтори: 200, тот же id, Idempotency-Replayed: true. Остаток списался только один раз.'},
 {title:'Склад не бесконечный',method:'POST',path:'/api/orders',status:[409],body:{productId:'{{productId}}',quantity:20},description:'Попробуй купить больше, чем осталось. Тело корректное, но текущее состояние склада не позволяет выполнить действие. Это конфликт, а не ошибка JSON.',check:'409, code: OUT_OF_STOCK. Предыдущий заказ и остаток не изменились.'},
 {title:'Платёж не прошёл',method:'POST',path:'/api/orders/{{orderId}}/pay',status:[402],body:{outcome:'DECLINED'},description:'Это симулятор, не платёжная система: банковские данные здесь не нужны. Принудительно отклони оплату и проверь, что заказ не стал оплаченным.',check:'402, PAYMENT_DECLINED. GET /api/orders покажет старый статус CREATED.'},
 {title:'Отмени заказ',method:'POST',path:'/api/orders/{{orderId}}/cancel',status:[200],description:'Отмена возвращает зарезервированный товар на склад. Повторная отмена безопасна: товар не должен возвращаться дважды.',check:'200, status: CANCELLED. В GET /api/products остаток снова 5. Повтори отмену: остаток не увеличится ещё раз.'},
 {title:'DELETE: удали товар',method:'DELETE',path:'/api/products/{{productId}}',status:[204],description:'Удаляем товар, созданный тобой в шаге 5. Отменённый заказ останется в истории. У успешного удаления нет JSON-тела.',check:'204 и пустой ответ. Повторный DELETE — 404: товар уже не существует.'},
 {title:'404: такого товара нет',method:'GET',path:'/api/products/not-found',status:[404],description:'Адрес API существует, но конкретного товара нет. Сервер должен объяснить это предсказуемо и не отвечать 500.',check:'404, PRODUCT_NOT_FOUND. В ошибке есть requestId; он совпадает с заголовком X-Request-Id.'},
 {title:'401: без пропуска',method:'GET',path:'/api/me',status:[401],noAuth:true,description:'Мы специально сняли галочку токена. Сервер не может определить пользователя. Верни галочку и повтори: теперь ожидай 200.',check:'Без токена — 401 и заголовок WWW-Authenticate: Bearer. С действующим токеном — 200.'},
 {title:'403: доступ запрещён',method:'GET',path:'/api/admin/stats',status:[403],description:'Ты зарегистрирован и роль у тебя USER. Админ-раздел требует ADMIN. В публичном тренажёре администратором стать нельзя — это упражнение на проверку прав.',check:'403, FORBIDDEN. Если убрать токен, получится 401. Это два разных состояния.'},
 {title:'Вход после перерыва',method:'POST',path:'/api/auth/login',status:[200],body:{email:'{{email}}',password:'LearnOnly123!'},description:'Регистрация создаёт пользователя, login выдаёт новый токен существующему пользователю. После обновления страницы прежний токен забывается — можно войти снова, если сервер не перезапускался.',check:'200 и новый token. С неправильным паролем — 401. Следующий уровень: импортируй коллекцию в Postman.'}
];
const expand = value => value.replace(/\{\{(email|productId|orderId)\}\}/g,(_,key)=>state[key]||`{{${key}}}`);
function renderMissions(){
 const missions=$('missions');
 missions.replaceChildren();
 lessons.forEach((lesson,index)=>{
  const button=document.createElement('button');button.type='button';button.className=`mission ${index===state.lesson?'active':''} ${state.completed.has(index)?'done':''}`;
  button.setAttribute('aria-current',index===state.lesson?'step':'false');
  const number=document.createElement('span');number.className='index';number.textContent=state.completed.has(index)?'✓':String(index+1).padStart(2,'0');
  const title=document.createElement('span');title.textContent=lesson.title;button.append(number,title);button.addEventListener('click',()=>choose(index));missions.append(button);
 });
 const progress=document.getElementById('progress');
 if(progress)progress.textContent=`${state.completed.size} / ${lessons.length}`;
 $('progress-fill').style.width=`${Math.round((state.completed.size/lessons.length)*100)}%`;
}
function choose(index){
 state.lesson=index;const lesson=lessons[index];
 $('lesson-label').textContent=`ЭКСПЕРИМЕНТ ${String(index+1).padStart(2,'0')} / ${lesson.method}`;
 $('lesson-title').textContent=lesson.title;$('lesson-description').textContent=lesson.description;$('lesson-check').textContent=lesson.check;
 $('method').value=lesson.method;$('path').value=expand(lesson.path);$('body').value=lesson.body?expand(JSON.stringify(lesson.body,null,2)):'';$('key').value=lesson.key||'';$('use-token').checked=!lesson.noAuth;
 bodyMode();renderMissions();
}
function bodyMode(){const disabled=['GET','HEAD','OPTIONS'].includes($('method').value);$('body').disabled=disabled;$('body-hint').textContent=disabled?'У этого запроса тело не отправляется.':'Текст из этого поля отправится серверу как JSON. Можно намеренно сломать его для проверки 400.';}
function syncToken(value){state.token=value;$('token').value=value;$('auth-state').textContent=value?'токен готов ✓':'ещё нет токена';$('auth-state').classList.toggle('ready',Boolean(value));}
async function send(){
 const method=$('method').value,path=expand($('path').value.trim()),body=expand($('body').value),lessonIndex=state.lesson,lesson=lessons[lessonIndex];
 if(!path.startsWith('/api/')||path.includes('\\')||new URL(path,location.origin).origin!==location.origin){$('feedback').textContent='Для безопасности доступны только пути /api/ этого сервера.';return;}
 if(path.includes('{{')||body.includes('{{')){$('feedback').textContent='Не хватает id. Сначала выполни предыдущие шаги: регистрация → товары → создание товара → заказ.';return;}
 const headers={};if($('use-token').checked&&state.token)headers.Authorization=`Bearer ${state.token}`;
 const hasBody=!['GET','HEAD','OPTIONS'].includes(method)&&body.trim()!=='';
 if(hasBody)headers['Content-Type']='application/json';if($('key').value.trim())headers['Idempotency-Key']=$('key').value.trim();
 const safeHeaders={...headers};if(safeHeaders.Authorization)safeHeaders.Authorization='Bearer <токен скрыт>';
 let safeBody=body;try{const parsed=JSON.parse(body);if('password' in parsed){parsed.password='<учебный пароль скрыт>';safeBody=JSON.stringify(parsed,null,2);}}catch{}
 $('raw-request').textContent=`${method} ${path}\n${Object.entries(safeHeaders).map(([k,v])=>`${k}: ${v}`).join('\n')}${hasBody?'\n\n'+safeBody:''}`;
 $('send').disabled=true;$('send').setAttribute('aria-busy','true');$('status').textContent='Отправляем…';$('status').className='status';$('feedback').textContent='Ожидаем ответ. После сна бесплатный сервер запускается заново.';
 const started=performance.now();const abort=new AbortController();const timeout=setTimeout(()=>abort.abort(),90000);
 let response,raw;
 try{
  response=await fetch(path,{method,headers,body:hasBody?body:undefined,signal:abort.signal,credentials:'omit',redirect:'error'});
  raw=await response.text();
 }catch(error){
  $('status').textContent='Нет ответа';$('status').className='status bad';$('feedback').textContent=error.name==='AbortError'?'Сервер не ответил за 90 секунд. Возможно, он ещё запускается. Попробуй снова.':'Не удалось получить ответ. Проверь сеть и доступность сервера.';$('response').textContent='HTTP-статус не получен. Это не то же самое, что ответ 500.';
  return;
 }finally{clearTimeout(timeout);$('send').disabled=false;$('send').removeAttribute('aria-busy');}
 try{
  let data;try{data=JSON.parse(raw);}catch{}
  $('status').textContent=`${response.status} ${response.statusText}`;$('status').className=`status ${response.ok?'good':'bad'}`;$('timing').textContent=`${Math.round(performance.now()-started)} мс`;
  let display=data;if(data?.token){syncToken(data.token);display={...data,token:'<сохранён в поле токена; скрыт в ответе>'};if(data.user?.email)state.email=data.user.email;}
  $('response').textContent=display?JSON.stringify(display,null,2):(raw||'∅ Пустое тело ответа. Для 204 это правильно.');
  $('copy-response').disabled=false;
  $('headers').textContent=Array.from(response.headers).map(([k,v])=>`${k}: ${v}`).join('\n');
  if(response.ok&&path.startsWith('/api/products')){if(data?.id)state.productId=data.id;else if(!state.productId&&data?.items?.length)state.productId=data.items[0].id;}
  if(response.ok&&path.startsWith('/api/orders')&&data?.id)state.orderId=data.id;
  if(response.ok&&(path==='/api/auth/logout'||(path==='/api/me'&&method==='DELETE')))syncToken('');
  const expected=lesson.status.includes(response.status)&&method===lesson.method&&path.split('?')[0]===expand(lesson.path).split('?')[0];
  if(expected){state.completed.add(lessonIndex);renderMissions();$('feedback').textContent='Нужный статус получен ✓ Теперь проверь содержимое по подсказке выше: один код ещё не доказывает, что всё правильно.';}
  else $('feedback').textContent=response.ok?'Запрос выполнен. Сравни результат с ожиданием.':`${data?.message||'Сервер вернул ошибку'}. Смотри code и fields ниже.`;
 }catch(error){console.error('Не удалось полностью отобразить HTTP-ответ',error);$('feedback').textContent='HTTP-ответ получен, но интерфейс не смог отобразить часть результата.';}
}
$('send').addEventListener('click',send);$('method').addEventListener('change',bodyMode);$('token').addEventListener('input',e=>syncToken(e.target.value.trim()));$('show-token').addEventListener('click',()=>{$('token').type=$('token').type==='password'?'text':'password';});
document.querySelectorAll('[data-scroll-to]').forEach(button=>button.addEventListener('click',()=>document.querySelector(button.dataset.scrollTo)?.scrollIntoView({behavior:'smooth',block:'start'})));
$('copy-response').addEventListener('click',async()=>{try{await navigator.clipboard.writeText($('response').textContent);const button=$('copy-response');const previous=button.textContent;button.textContent='Скопировано ✓';setTimeout(()=>{button.textContent=previous;},1400);}catch{$('feedback').textContent='Не удалось скопировать автоматически. Выдели ответ и скопируй вручную.';}});
document.addEventListener('keydown',e=>{if((e.metaKey||e.ctrlKey)&&e.key==='Enter'&&!$('send').disabled){e.preventDefault();send();}});
$('reset').addEventListener('click',async()=>{
 if(!state.token){$('feedback').textContent='Сначала зарегистрируйся или войди.';return;}
 if(!confirm('Удалить только твои учебные товары и заказы и восстановить 3 исходных товара?'))return;
 try{const response=await fetch('/api/sandbox/reset',{method:'POST',headers:{Authorization:`Bearer ${state.token}`},credentials:'omit'});
  if(!response.ok){$('feedback').textContent='Сброс не выполнен. Войди заново и повтори.';return;}
  state.productId='';state.orderId='';state.completed.clear();choose(3);$('feedback').textContent='Твоя песочница сброшена. Запроси товары, чтобы получить новые id.';
 }catch{$('feedback').textContent='Нет связи с сервером. Сброс не подтверждён.';}
});
choose(0);
fetch('/api/health',{signal:AbortSignal.timeout(90000)}).then(r=>{if(!r.ok)throw Error();return r.json();}).then(data=>{$('health').textContent=`● API онлайн · ${data.version}`;$('health').classList.add('ready');}).catch(()=>{$('health').textContent='○ Сервер недоступен';});
