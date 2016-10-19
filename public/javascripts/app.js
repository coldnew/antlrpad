function App() {
    this.parseUrl = 'api/parse/';
    this.loadUrl = 'api/load/';
    this.saveUrl = 'api/save/';
};

App.prototype.loadSession = function() {
    var self = this;
    var sessionId = location.hash.substring(1);
    if (sessionId) {
        self.load(sessionId);
    }
};

App.prototype.init = function() {
    var self = this;

    self.loadSession();

    $('#parse').click(function() {
        self.parseExpression(self.parseUrl);
    });

    $('#save').click(function() {
        self.parseExpression(self.saveUrl, function(id){
            window.location = '/#' + id;
        });
    });

    var reqTimer;
    $('#src').on('keyup', function() {
        if (reqTimer) {
            clearTimeout(reqTimer);
        }

        reqTimer = setTimeout(function(){
            self.parseExpression(self.parseUrl);
        }, 1000);
    });

    window.onhashchange = function() {
        self.loadSession();
    };

    self.parserEditor = self.initEditor("grammar");
    self.lexerEditor = self.initEditor("lexer");
};

App.prototype.loadRules = function(rules, rule) {
    var startRuleSel = $('#startRule');
    startRuleSel.empty();
    for(var o in rules) {
        startRuleSel.append('<option>' + rules[o] + '</option>');
    }

    startRuleSel.val(rule);
};

App.prototype.showGrammarErrors = function(errors) {
    var getErrorView = function(e) {
       return {
           column: e.col,
           row: e.line - 1,
           type: e.errType,
           text: e.message
       }
    };

    var parserMessages = errors.filter(function(x){ return x.source == "parser" }).map(getErrorView);
    var lexerMessages = errors.filter(function(x){ return x.source == "lexer" }).map(getErrorView);

    this.parserEditor.getSession().setAnnotations(parserMessages);
    this.lexerEditor.getSession().setAnnotations(lexerMessages);
};

App.prototype.showParseMessages = function(messages) {
    if (messages.length == 0) {
        $('#parseMessages').hide();
    }
    else
    {
        var msg = messages.map(function(m) { return "line " + m.line + ", col: " + m.col + " : " + m.message; }).join("<br>");
        $('#parseMessages').text(msg).show();
    }
}

App.prototype.showParsedExpression = function(parsed) {
    var self = this;
    var data = parsed;
    if (data && data.tree && data.grammar.rules) {
        self.loadRules(data.grammar.rules, data.rule);
        self.draw(self.getTreeModel(data.tree));
        $('#grammarError').hide();
        self.showGrammarErrors(data.grammar.warnings);
        self.showParseMessages(data.messages);
        $('a[href="#treeTab"]').tab('show');
    } else {
        $('#grammarError').show();
        self.showGrammarErrors(data.grammar.errors);
        self.draw(null);
        $('a[href="#grammarTab"]').tab('show');
    }
}

App.prototype.parseExpression = function(url, callback) {
    var self = this;
    var grammar = self.parserEditor.getValue();
    var lexer = self.lexerEditor.getValue();
    var src = $('#src').val();
    var startRuleSel = $('#startRule');
    $.post(url, { grammar: grammar, lexer: lexer, src: src, rule: startRuleSel.val() }, function(resp){
        self.showParsedExpression(resp.parsed, callback);
        if (callback && resp.saved && resp.saved.id) {
            callback(resp.saved.id);
        }
    }).fail(function(err){
        $('#grammarError').show();
    });
}

App.prototype.load = function(id) {
    var self = this;
    $.get(self.loadUrl + id, {}, function(resp){
        var res = resp.loaded;
        self.parserEditor.setValue(res.grammarSrc, -1);
        self.lexerEditor.setValue(res.lexerSrc, -1);
        $('#src').val(res.src);
        self.showParsedExpression(resp.parsed);
    });
};

App.prototype.draw = function(tree) {
    $('#tree').jstree();
    $('#tree').jstree(true).settings.core.data = tree;
    $('#tree').jstree(true).refresh();
};

App.prototype.initEditor = function(elId) {
    var editor = ace.edit(elId);
    editor.setTheme("ace/theme/chrome");
    editor.getSession().setMode("ace/mode/antlr4");

    return editor;
};

App.prototype.getTreeModel = function(tree) {
    var children = [];
    for(var c in tree.children) {
        children.push(this.getTreeModel(tree.children[c]));
    }

    var node = { text: tree.rule + ': ' + tree.text, children: children, state: { opened: true }, icon: false };
    if (tree.hasError) {
        node.icon = "glyphicon glyphicon-remove red";
    }

    return node;
};


$(function(){
    var app = new App();
    app.init();
})