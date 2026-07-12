const state = {
  models: [],
  defaultModel: null,
  defaultThinkingEnabled: false
};

const elements = {
  payloadFile: document.getElementById('payloadFile'),
  metadataFile: document.getElementById('metadataFile'),
  fieldDescriptionsFile: document.getElementById('fieldDescriptionsFile'),
  checklistFile: document.getElementById('checklistFile'),
  modelSelect: document.getElementById('modelSelect'),
  modelDetails: document.getElementById('modelDetails'),
  thinkingToggle: document.getElementById('thinkingToggle'),
  thinkingHint: document.getElementById('thinkingHint'),
  analyzeButton: document.getElementById('analyzeButton'),
  requestStatus: document.getElementById('requestStatus'),
  runtimeStatus: document.getElementById('runtimeStatus'),
  report: document.getElementById('report'),
  reportModel: document.getElementById('reportModel'),
  clearAll: document.getElementById('clearAll')
};

const fileInputs = [
  { input: elements.payloadFile, name: document.getElementById('payloadFileName') },
  { input: elements.metadataFile, name: document.getElementById('metadataFileName') },
  { input: elements.fieldDescriptionsFile, name: document.getElementById('fieldDescriptionsFileName') },
  { input: elements.checklistFile, name: document.getElementById('checklistFileName') }
];

function setRuntimeStatus(message, status) {
  elements.runtimeStatus.className = `runtime-status ${status || ''}`.trim();
  elements.runtimeStatus.lastElementChild.textContent = message;
}

function setRequestStatus(message, isError = false) {
  elements.requestStatus.textContent = message;
  elements.requestStatus.className = `request-status${isError ? ' error' : ''}`;
}

function setLoading(loading) {
  elements.analyzeButton.disabled = loading;
  elements.analyzeButton.classList.toggle('loading', loading);
  elements.analyzeButton.querySelector('.button-label').textContent = loading ? 'Analiz ediliyor' : 'Analiz Et';
}

function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '';
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)} GB`;
}

function selectedModel() {
  return state.models.find(model => model.name === elements.modelSelect.value);
}

function updateModelControls() {
  const model = selectedModel();
  if (!model) {
    elements.modelDetails.textContent = 'Model bilgisi bulunamadı.';
    elements.thinkingToggle.checked = false;
    elements.thinkingToggle.disabled = true;
    return;
  }

  const details = [model.parameterSize, model.quantizationLevel, formatBytes(model.sizeBytes)].filter(Boolean);
  elements.modelDetails.textContent = `${details.join(' · ')}${model.defaultModel ? ' · Önerilen varsayılan' : ''}`;
  elements.thinkingToggle.disabled = !model.supportsThinking;

  if (!model.supportsThinking) {
    elements.thinkingToggle.checked = false;
    elements.thinkingHint.textContent = 'Bu model Ollama thinking yeteneği bildirmiyor.';
  } else {
    elements.thinkingHint.textContent = 'Daha uzun sürebilir; reasoning kullanıcıya gösterilmez.';
  }
}

async function loadModels() {
  try {
    const response = await fetch('/api/models');
    const data = await response.json();
    if (!response.ok) throw new Error(data.details || data.error || 'Model listesi alınamadı.');

    state.models = data.models || [];
    state.defaultModel = data.defaultModel;
    state.defaultThinkingEnabled = data.defaultThinkingEnabled;
    elements.modelSelect.replaceChildren();

    if (!state.models.length) {
      const option = new Option('Kurulu model bulunamadı', '');
      elements.modelSelect.add(option);
      elements.modelSelect.disabled = true;
      setRuntimeStatus('Kurulu model bulunamadı', 'offline');
      return;
    }

    state.models.forEach(model => {
      const label = `${model.name}${model.defaultModel ? ' (önerilen)' : ''}`;
      elements.modelSelect.add(new Option(label, model.name));
    });

    elements.modelSelect.value = state.models.some(model => model.name === state.defaultModel)
      ? state.defaultModel
      : state.models[0].name;
    elements.modelSelect.disabled = false;
    elements.thinkingToggle.checked = state.defaultThinkingEnabled;
    updateModelControls();
    setRuntimeStatus(`${state.models.length} model hazır`, 'online');
  } catch (error) {
    elements.modelSelect.replaceChildren(new Option('Ollama kullanılamıyor', ''));
    elements.modelSelect.disabled = true;
    elements.thinkingToggle.disabled = true;
    elements.modelDetails.textContent = error.message;
    setRuntimeStatus('Ollama bağlantısı yok', 'offline');
  }
}

function updateFileControl(input, nameElement) {
  const file = input.files[0];
  nameElement.textContent = file ? file.name : 'Dosya seçilmedi';
  input.closest('.file-control').classList.toggle('has-file', Boolean(file));
}

async function readJson(input, required = false) {
  const file = input.files[0];
  if (!file) {
    if (required) throw new Error('Issue JSON dosyası seçilmelidir.');
    return undefined;
  }

  try {
    return JSON.parse(await file.text());
  } catch (error) {
    throw new Error(`${file.name} geçerli bir JSON dosyası değil.`);
  }
}

function addText(parent, tag, value, className) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  element.textContent = value;
  parent.append(element);
  return element;
}

function addEvidence(parent, evidence) {
  if (!evidence || !evidence.length) return;
  const list = document.createElement('ul');
  list.className = 'evidence-list';
  evidence.forEach(item => addText(list, 'li', item));
  parent.append(list);
}

function renderFinding(parent, finding) {
  const card = document.createElement('article');
  card.className = `finding ${finding.severity.toLowerCase()}`;
  const heading = document.createElement('div');
  heading.className = 'card-heading';
  addText(heading, 'h4', finding.title);
  addText(heading, 'span', finding.severity, 'severity');
  card.append(heading);
  addText(card, 'p', finding.category, 'card-category');
  addText(card, 'p', finding.rationale, 'card-body');
  addEvidence(card, finding.evidence);
  const action = addText(card, 'p', '', 'action');
  addText(action, 'strong', 'Önerilen aksiyon: ');
  action.append(document.createTextNode(finding.recommendedAction));
  parent.append(card);
}

function renderObservation(parent, observation) {
  const card = document.createElement('article');
  card.className = 'observation';
  addText(card, 'h4', observation.type);
  addText(card, 'p', observation.description, 'card-body');
  addEvidence(card, observation.evidence);
  parent.append(card);
}

function reportSection(parent, title) {
  const section = document.createElement('section');
  section.className = 'report-section';
  addText(section, 'h3', title, 'section-title');
  parent.append(section);
  return section;
}

function addRawOutput(parent, output, label = 'Ham model çıktısı') {
  const details = document.createElement('details');
  details.className = 'raw-output';
  addText(details, 'summary', label);
  addText(details, 'pre', output || 'Boş çıktı');
  parent.append(details);
}

function renderReport(data, modelName) {
  const report = data.report;
  const root = elements.report;
  root.className = '';
  root.replaceChildren();

  addText(root, 'p', report.summary, 'report-summary');
  const metrics = document.createElement('div');
  metrics.className = 'report-metrics';
  addText(metrics, 'span', `${report.findings.length} bulgu`, 'metric');
  addText(metrics, 'span', `${report.observations.length} gözlem`, 'metric');
  if (elements.thinkingToggle.checked) addText(metrics, 'span', 'Thinking açık', 'metric');
  root.append(metrics);

  const findings = reportSection(root, 'Bulgular');
  if (report.findings.length) report.findings.forEach(finding => renderFinding(findings, finding));
  else addText(findings, 'p', 'Desteklenen denetim bulgusu yok.', 'section-empty');

  if (report.observations.length) {
    const observations = reportSection(root, 'Gözlemler ve yetersiz bağlam');
    report.observations.forEach(observation => renderObservation(observations, observation));
  }

  const recommendation = reportSection(root, 'Son öneri');
  addText(recommendation, 'div', report.recommendation, 'recommendation');
  addRawOutput(root, data.agentOutput);

  elements.reportModel.textContent = modelName;
  elements.reportModel.hidden = false;
}

function renderValidationFailure(data, modelName) {
  const root = elements.report;
  root.className = '';
  root.replaceChildren();
  const warning = document.createElement('div');
  warning.className = 'validation-warning';
  addText(warning, 'h3', 'Rapor yapısı doğrulanamadı');
  addText(warning, 'p', (data.reportValidationErrors || []).join(' ') || 'Model geçerli rapor üretemedi.');
  root.append(warning);
  addRawOutput(root, data.agentOutput, 'İncelemek için ham çıktıyı aç');
  elements.reportModel.textContent = modelName;
  elements.reportModel.hidden = false;
}

function resetReport() {
  elements.report.className = 'report-empty';
  elements.report.replaceChildren();
  addText(elements.report, 'div', 'AI', 'empty-mark');
  addText(elements.report, 'h3', 'Analiz bekleniyor');
  addText(elements.report, 'p', 'Issue dosyasını ve kullanmak istediğin modeli seçerek analizi başlat.');
  elements.reportModel.hidden = true;
  elements.reportModel.textContent = '';
}

async function analyze() {
  try {
    setLoading(true);
    setRequestStatus('Dosyalar okunuyor...');

    const issue = await readJson(elements.payloadFile, true);
    const metadata = await readJson(elements.metadataFile);
    const fieldDescriptions = await readJson(elements.fieldDescriptionsFile);
    const checklist = await readJson(elements.checklistFile);
    const modelName = elements.modelSelect.value;

    if (!modelName) throw new Error('Analiz için kurulu bir model seçilmelidir.');

    const body = {
      payload: issue.payload || issue,
      metadata: metadata ?? issue.metadata,
      fieldDescriptions: fieldDescriptions ?? issue.fieldDescriptions,
      checklist: checklist ?? issue.checklist,
      agentOptions: {
        model: modelName,
        thinkingEnabled: elements.thinkingToggle.checked
      }
    };

    setRequestStatus(`${modelName} ile analiz çalışıyor...`);
    const response = await fetch('/api/analyze', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.details || data.error || 'Analiz tamamlanamadı.');

    if (data.structuredOutput) {
      renderReport(data, modelName);
      setRequestStatus('Analiz tamamlandı.');
    } else {
      renderValidationFailure(data, modelName);
      setRequestStatus('Model çıktısı doğrulama dışında bırakıldı.', true);
    }
  } catch (error) {
    setRequestStatus(error.message, true);
  } finally {
    setLoading(false);
  }
}

function clearFiles() {
  fileInputs.forEach(({ input, name }) => {
    input.value = '';
    updateFileControl(input, name);
  });
  setRequestStatus('');
  resetReport();
}

fileInputs.forEach(({ input, name }) => {
  input.addEventListener('change', () => updateFileControl(input, name));
});
elements.modelSelect.addEventListener('change', updateModelControls);
elements.analyzeButton.addEventListener('click', analyze);
elements.clearAll.addEventListener('click', clearFiles);

loadModels();
