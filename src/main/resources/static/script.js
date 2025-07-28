document.addEventListener('DOMContentLoaded', function () {
    // DOM Elements
    const jobDescriptionTextarea = document.getElementById('jobDescription');
    const cvContentTextarea = document.getElementById('cvContent');
    const jdWordCount = document.getElementById('jdWordCount');
    const cvWordCount = document.getElementById('cvWordCount');
    const analyzeBtn = document.getElementById('analyzeBtn');
    const resetBtn = document.getElementById('resetBtn');
    const resultsSection = document.getElementById('resultsSection');
    const matchPercentage = document.getElementById('matchPercentage');
    const matchedKeywordsList = document.getElementById('matchedKeywords');
    const missingKeywordsList = document.getElementById('missingKeywords');
    const matchedSkillsList = document.getElementById('matchedSkills');
    const missingSkillsList = document.getElementById('missingSkills');
    const optimizationTipsList = document.getElementById('optimizationTips');
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    const percentageCircle = document.querySelector('.percentage');
    const circleFill = document.querySelector('.circle-fill');
    const cvFileInput = document.getElementById('cvFile');
    const toggleTextBtn = document.getElementById('toggleTextBtn');
    const fileInfo = document.getElementById('fileInfo');
    const fileName = document.getElementById('fileName');
    const removeFileBtn = document.getElementById('removeFileBtn');
    const disclaimerModal = document.getElementById('disclaimerModal');
    const acceptDisclaimerBtn = document.getElementById('acceptDisclaimer');

    // Word count functionality
    jobDescriptionTextarea.addEventListener('input', updateWordCount);
    cvContentTextarea.addEventListener('input', updateWordCount);

    function updateWordCount() {
        const jdText = jobDescriptionTextarea.value.trim();
        const cvText = cvContentTextarea.value.trim();

        jdWordCount.textContent = jdText === '' ? '0' : jdText.split(/\s+/).length;
        cvWordCount.textContent = cvText === '' ? '0' : cvText.split(/\s+/).length;
    }

    // Tab switching functionality
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabId = btn.getAttribute('data-tab');

            // Update active tab button
            tabBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            // Update active tab content
            tabContents.forEach(content => content.classList.remove('active'));
            document.getElementById(`${tabId}-tab`).classList.add('active');
        });
    });

    // Reset functionality
    resetBtn.addEventListener('click', () => {
        jobDescriptionTextarea.value = '';
        cvContentTextarea.value = '';
        jdWordCount.textContent = '0';
        cvWordCount.textContent = '0';
        resultsSection.style.display = 'none';
        removeUploadedFile();
    });

    // File upload functionality
    cvFileInput.addEventListener('change', handleFileUpload);
    toggleTextBtn.addEventListener('click', toggleTextInput);
    removeFileBtn.addEventListener('click', removeUploadedFile);

    function handleFileUpload(event) {
        const file = event.target.files[0];
        if (!file)
            return;

        // Validate file size (5MB limit)
        if (file.size > 5 * 1024 * 1024) {
            alert('File size exceeds 5MB limit');
            cvFileInput.value = '';
            return;
        }

        // Validate file type
        const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
        if (!validTypes.includes(file.type) && !file.name.match(/\.(pdf|doc|docx)$/i)) {
            alert('Please upload a PDF or Word document');
            cvFileInput.value = '';
            return;
        }

        // Display file info
        fileName.textContent = file.name;
        fileInfo.style.display = 'flex';
        cvContentTextarea.style.display = 'none';
        cvContentTextarea.value = '';

        //extract text
        extractTextFromFile(file)
                .then(text => {
                    cvContentTextarea.value = text;
                    updateWordCount();
                })
                .catch(error => {
                    console.error('Error extracting text:', error);
                    alert('Error processing file. Please try another file or paste text instead.');
                    removeUploadedFile();
                });
    }

    function toggleTextInput() {
        if (cvContentTextarea.style.display === 'none') {
            cvContentTextarea.style.display = 'block';
            fileInfo.style.display = 'none';
            cvFileInput.value = '';
            cvContentTextarea.focus();
        } else {
            cvContentTextarea.style.display = 'none';
        }
    }

    function removeUploadedFile() {
        cvFileInput.value = '';
        fileInfo.style.display = 'none';
        cvContentTextarea.value = '';
        cvContentTextarea.style.display = 'none';
        updateWordCount();
    }

    async function extractTextFromFile(file) {
        if (file.name.match(/\.pdf$/i)) {
            return await extractTextFromPDF(file);
        } else if (file.name.match(/\.docx?$/i)) {
            return await extractTextFromWord(file);
        } else {
            throw new Error('Unsupported file format');
        }
    }

    async function extractTextFromPDF(file) {
        if (typeof pdfjsLib === 'undefined') {
            await loadScript('https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.11.338/pdf.min.js');
            pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.11.338/pdf.worker.min.js';
        }

        return new Promise((resolve, reject) => {
            const fileReader = new FileReader();

            fileReader.onload = async function () {
                try {
                    const typedArray = new Uint8Array(this.result);
                    const loadingTask = pdfjsLib.getDocument(typedArray);
                    const pdf = await loadingTask.promise;

                    let fullText = '';
                    for (let i = 1; i <= pdf.numPages; i++) {
                        const page = await pdf.getPage(i);
                        const textContent = await page.getTextContent();
                        const textItems = textContent.items.map(item => item.str);
                        fullText += textItems.join(' ') + '\n';
                    }

                    resolve(fullText);
                } catch (error) {
                    reject(error);
                }
            };

            fileReader.onerror = reject;
            fileReader.readAsArrayBuffer(file);
        });
    }

    async function extractTextFromWord(file) {
        if (typeof mammoth === 'undefined') {
            await loadScript('https://cdnjs.cloudflare.com/ajax/libs/mammoth/1.4.0/mammoth.browser.min.js');
        }

        return new Promise((resolve, reject) => {
            const fileReader = new FileReader();

            fileReader.onload = async function () {
                try {
                    const arrayBuffer = this.result;
                    const result = await mammoth.extractRawText({arrayBuffer});
                    resolve(result.value);
                } catch (error) {
                    reject(error);
                }
            };

            fileReader.onerror = reject;
            fileReader.readAsArrayBuffer(file);
        });
    }

    function loadScript(src) {
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.onload = resolve;
            script.onerror = reject;
            document.head.appendChild(script);
        });
    }

    //analyze functionality
    analyzeBtn.addEventListener('click', analyzeDocuments);

    function analyzeDocuments() {
        const jobDescription = jobDescriptionTextarea.value.trim();
        const cvContent = cvContentTextarea.value.trim();
        const cvFile = cvFileInput.files[0];

        if (!jobDescription) {
            alert('Please enter a job description');
            return;
        }

        if (!cvContent && !cvFile) {
            alert('Please upload a CV file or paste your CV content');
            return;
        }

        // loading state
        analyzeBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Analyzing...';
        analyzeBtn.disabled = true;

        //prep  form data
        const formData = new FormData();
        formData.append('jobDescription', jobDescription);

        if (cvFile) {
            formData.append('cvFile', cvFile);
        } else {
            formData.append('cvContent', cvContent);
        }

        // backend API
        fetch('http://localhost:8080/api/analysis', {
            method: 'POST',
            body: formData
        })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Network response was not ok');
                    }
                    return response.json();
                })
                .then(data => {
                    displayResults(data);
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('An error occurred during analysis. Please try again.');
                })
                .finally(() => {
                    // reset the buttons state
                    analyzeBtn.innerHTML = '<i class="fas fa-chart-bar"></i> Analyze Match';
                    analyzeBtn.disabled = false;
                });
    }

    function displayResults(results) {
        //console.log("Analysis Results:", results);

        // Show the industry
        const industryDisplay = document.createElement('div');
        industryDisplay.className = 'industry-tag';
        industryDisplay.innerHTML = `<i class="fas fa-industry"></i> Detected Industry: 
            <span class="industry-name">${results.detectedIndustry || 'Not specified'}</span>`;

        // Clear previous industry display
        const existingIndustry = document.querySelector('.match-score .industry-tag');
        if (existingIndustry) {
            existingIndustry.remove();
        }
        document.querySelector('.match-score').appendChild(industryDisplay);

        // Update percentage
        matchPercentage.textContent = results.matchPercentage;
        percentageCircle.textContent = `${results.matchPercentage}%`;

        // Update animation
        const percentage = results.matchPercentage;
        circleFill.style.setProperty('--percentage', percentage);
        circleFill.setAttribute('stroke-dasharray', `${percentage}, 100`);

        // Display matching keywords
        matchedKeywordsList.innerHTML = '';
        if (results.matchedKeywords && results.matchedKeywords.length > 0) {
            results.matchedKeywords.forEach(keyword => {
                const keywordElement = document.createElement('span');
                keywordElement.className = 'keyword matched';
                keywordElement.textContent = keyword;
                matchedKeywordsList.appendChild(keywordElement);
            });
        } else {
            matchedKeywordsList.innerHTML = '<p class="no-results">No keyword matches found</p>';
        }

        // Display missing keywords
        missingKeywordsList.innerHTML = '';
        if (results.missingKeywords && results.missingKeywords.length > 0) {
            results.missingKeywords.forEach(keyword => {
                const keywordElement = document.createElement('span');
                keywordElement.className = 'keyword missing';
                keywordElement.textContent = keyword;
                missingKeywordsList.appendChild(keywordElement);
            });
        } else {
            missingKeywordsList.innerHTML = '<p class="no-results">All important keywords matched</p>';
        }

        // Display matched skills - with enhanced handling
        const matchedSkillsContainer = document.getElementById('matchedSkills');
        matchedSkillsContainer.innerHTML = '';
        if (results.matchedSkills && results.matchedSkills.length > 0) {
            results.matchedSkills.forEach(skill => {
                const skillElement = document.createElement('span');
                skillElement.className = 'skill matched';
                skillElement.textContent = skill;
                matchedSkillsContainer.appendChild(skillElement);
            });
        } else {
            matchedSkillsContainer.innerHTML = '<p class="no-results">No directly matched skills found</p>';
        }

        // Display missing skills - with enhanced handling
        const missingSkillsContainer = document.getElementById('missingSkills');
        missingSkillsContainer.innerHTML = '';
        if (results.missingSkills && results.missingSkills.length > 0) {
            results.missingSkills.forEach(skill => {
                const skillElement = document.createElement('span');
                skillElement.className = 'skill missing';
                skillElement.textContent = skill;
                missingSkillsContainer.appendChild(skillElement);
            });
        } else {
            missingSkillsContainer.innerHTML = '<p class="no-results">No important skills missing</p>';
        }

        // Display optimization tips
        optimizationTipsList.innerHTML = '';
        if (results.optimizationTips && results.optimizationTips.length > 0) {
            results.optimizationTips.forEach(tip => {
                const tipElement = document.createElement('div');
                tipElement.className = 'suggestion';
                tipElement.textContent = tip;
                optimizationTipsList.appendChild(tipElement);
            });
        } else {
            optimizationTipsList.innerHTML = '<p class="no-results">No optimization tips available</p>';
        }

        // Show results section
        resultsSection.style.display = 'block';

        // Scroll to results
        resultsSection.scrollIntoView({behavior: 'smooth'});
    }

    if (!localStorage.getItem('disclaimerAccepted')) {
        disclaimerModal.style.display = 'block';
    }

    // Close disclaimer
    acceptDisclaimerBtn.addEventListener('click', () => {
        disclaimerModal.style.display = 'none';
        localStorage.setItem('disclaimerAccepted', 'true');
    });

    const footer = document.querySelector('footer');
    const disclaimerLink = document.createElement('a');
    disclaimerLink.href = '#';
    disclaimerLink.textContent = 'View Disclaimer';
    disclaimerLink.style.marginLeft = '10px';
    disclaimerLink.style.color = 'var(--primary-color)';
    disclaimerLink.addEventListener('click', (e) => {
        e.preventDefault();
        disclaimerModal.style.display = 'block';
    });
    footer.appendChild(disclaimerLink);
});
